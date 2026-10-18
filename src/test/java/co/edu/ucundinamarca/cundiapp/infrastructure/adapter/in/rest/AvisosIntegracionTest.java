package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import co.edu.ucundinamarca.cundiapp.EnviadorDePushDePrueba;
import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.in.EntregarAvisosPendientes;
import co.edu.ucundinamarca.cundiapp.application.port.in.GenerarAvisosDeVencimiento;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeEnvioPush;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Recibir avisos de vencimiento (RF10, SCRUM-37), con seguridad real y PostgreSQL real. El envío de Web Push
 * es el de prueba: anota lo que se le pide y responde lo que cada caso decida.
 *
 * <p>Lo que hay que demostrar: el aviso sale una sola vez, solo para lo que está dentro de la anticipación y
 * de quien lo tiene encendido; siempre se ve dentro de la aplicación, llegue o no al dispositivo; una
 * suscripción muerta se desactiva; un fallo pasajero se reintenta pocas veces; y nada de esto cruza de un
 * estudiante a otro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AvisosIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String CHROME_ANDROID =
			"Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";
	private static final ClavesDePushDePrueba.Suscripcion CLAVES = ClavesDePushDePrueba.nueva();

	/** Quien entró: su token y su identificador, para mirar la base de datos de lo suyo y nada más. */
	private record Sesion(String token, int id, String endpoint) {
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private GenerarAvisosDeVencimiento generar;

	@Autowired
	private EntregarAvisosPendientes entregar;

	@Autowired
	private EnviadorDePushDePrueba push;

	@BeforeEach
	void limpiarElEnvioDePrueba() {
		push.reiniciar();
	}

	private Sesion entrar(String usuario) throws Exception {
		String correo = usuario + "@ucundinamarca.edu.co";
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true, Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		MvcResult login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return new Sesion(JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso"), creada.id(),
				"https://fcm.googleapis.com/fcm/send/" + usuario);
	}

	private int crearPendiente(Sesion sesion, String titulo, Instant vence) throws Exception {
		MvcResult creado = mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + sesion.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\":\"%s\",\"fechaLimite\":\"%s\"}".formatted(titulo, vence)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
	}

	private Instant enHoras(long horas) {
		return Instant.now().plus(horas, ChronoUnit.HOURS);
	}

	private ResultActions suscribir(Sesion sesion, String cuerpo) throws Exception {
		return mvc.perform(put("/api/mis/dispositivos/suscripcion").header("Authorization", "Bearer " + sesion.token())
				.header("User-Agent", CHROME_ANDROID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	private String suscripcionJson(String endpoint) {
		return "{\"endpoint\":\"%s\",\"keys\":{\"p256dh\":\"%s\",\"auth\":\"%s\"}}"
				.formatted(endpoint, CLAVES.clavePublica(), CLAVES.secretoEnBase64());
	}

	private void conDispositivo(Sesion sesion) throws Exception {
		suscribir(sesion, suscripcionJson(sesion.endpoint())).andExpect(status().isOk());
	}

	private ResultActions avisos(Sesion sesion) throws Exception {
		return mvc.perform(get("/api/mis/avisos").header("Authorization", "Bearer " + sesion.token()));
	}

	private List<EnviadorDePushDePrueba.Envio> enviosA(Sesion sesion) {
		return push.envios().stream().filter(e -> e.suscripcion().endpoint().equals(sesion.endpoint())).toList();
	}

	private Map<String, Object> filaDelAviso(Sesion sesion) {
		return jdbc.queryForMap("SELECT estado_envio, intentos, leida FROM notificacion WHERE id_estudiante = ?", sesion.id());
	}

	// --- Generar ---------------------------------------------------------------------------------------------

	@Test
	void unPendienteQueVenceDentroDeLaAnticipacionGeneraSuAvisoUnaSolaVez() throws Exception {
		Sesion ana = entrar("avisos.una.vez");
		crearPendiente(ana, "Taller de redes", enHoras(10));

		generar.ejecutar();
		generar.ejecutar();
		generar.ejecutar();

		avisos(ana)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.avisos.length()").value(1))
				.andExpect(jsonPath("$.sinLeer").value(1))
				.andExpect(jsonPath("$.avisos[0].tipo").value("vencimiento_proximo"))
				.andExpect(jsonPath("$.avisos[0].titulo").value("Vence pronto"))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.containsString("«Taller de redes» vence")))
				.andExpect(jsonPath("$.avisos[0].leido").value(false));
	}

	@Test
	void unPendienteLejanoNoGeneraAviso() throws Exception {
		Sesion ana = entrar("avisos.lejano");
		crearPendiente(ana, "Proyecto final", enHoras(24 * 10));

		generar.ejecutar();

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void unPendienteYaCumplidoNoGeneraAviso() throws Exception {
		Sesion ana = entrar("avisos.cumplido");
		int id = crearPendiente(ana, "Lectura", enHoras(5));
		mvc.perform(patch("/api/mis/pendientes/" + id).header("Authorization", "Bearer " + ana.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"cumplido\":true}"))
				.andExpect(status().isOk());

		generar.ejecutar();

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void conLosAvisosApagadosNoSeGeneraNada() throws Exception {
		Sesion ana = entrar("avisos.apagados");
		jdbc.update("UPDATE configuracion_estudiante SET notificaciones_activas = FALSE WHERE id_estudiante = ?", ana.id());
		crearPendiente(ana, "Taller", enHoras(5));

		generar.ejecutar();

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void laAnticipacionDelEstudianteDecideCuandoSeAvisa() throws Exception {
		Sesion ana = entrar("avisos.anticipacion");
		jdbc.update("UPDATE configuracion_estudiante SET anticipacion_aviso_horas = 2 WHERE id_estudiante = ?", ana.id());
		crearPendiente(ana, "Lejos para ella", enHoras(10));
		crearPendiente(ana, "Cerca", enHoras(1));

		generar.ejecutar();

		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(1))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.containsString("«Cerca»")));
	}

	// --- Entregar --------------------------------------------------------------------------------------------

	@Test
	void unAvisoSeEntregaAlDispositivoSuscritoYQuedaEnviado() throws Exception {
		Sesion ana = entrar("avisos.entrega");
		conDispositivo(ana);
		crearPendiente(ana, "Taller de redes", enHoras(10));

		generar.ejecutar();
		entregar.ejecutar();

		assertThat(enviosA(ana)).hasSize(1);
		var envio = enviosA(ana).getFirst();
		assertThat(envio.carga().titulo()).isEqualTo("Vence pronto");
		assertThat(envio.carga().mensaje()).contains("«Taller de redes»");
		assertThat(envio.carga().ruta()).isEqualTo("/pendientes");
		assertThat(filaDelAviso(ana)).containsEntry("estado_envio", "enviada").containsEntry("intentos", 1);
		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(1));
	}

	@Test
	void sinDispositivoElAvisoQuedaFallidoPeroSigueVisibleEnLaAplicacion() throws Exception {
		Sesion ana = entrar("avisos.sin.dispositivo");
		crearPendiente(ana, "Taller", enHoras(10));

		generar.ejecutar();
		entregar.ejecutar();

		assertThat(enviosA(ana)).isEmpty();
		assertThat(filaDelAviso(ana)).containsEntry("estado_envio", "fallida");
		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(1))
				.andExpect(jsonPath("$.sinLeer").value(1));
	}

	@Test
	void sinWebPushEnElServidorElAvisoSoloSeVeDentroDeLaAplicacion() throws Exception {
		Sesion ana = entrar("avisos.sin.push");
		conDispositivo(ana);
		push.estaDisponible(false);
		crearPendiente(ana, "Taller", enHoras(10));

		generar.ejecutar();
		entregar.ejecutar();

		assertThat(enviosA(ana)).isEmpty();
		assertThat(filaDelAviso(ana)).containsEntry("estado_envio", "fallida");
		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(1));
	}

	@Test
	void unaSuscripcionMuertaSeDesactivaYElAvisoQuedaFallido() throws Exception {
		Sesion ana = entrar("avisos.muerta");
		conDispositivo(ana);
		push.responder(ResultadoDeEnvioPush.SUSCRIPCION_MUERTA);
		crearPendiente(ana, "Taller", enHoras(10));

		generar.ejecutar();
		entregar.ejecutar();

		Map<String, Object> dispositivo = jdbc.queryForMap(
				"SELECT activo, endpoint_push, clave_p256dh FROM dispositivo WHERE id_estudiante = ?", ana.id());
		assertThat(dispositivo).containsEntry("activo", false).containsEntry("endpoint_push", null).containsEntry("clave_p256dh", null);
		assertThat(filaDelAviso(ana)).containsEntry("estado_envio", "fallida");
	}

	@Test
	void unFalloPasajeroSeReintentaPocasVecesYLuegoQuedaFallido() throws Exception {
		Sesion ana = entrar("avisos.pasajero");
		conDispositivo(ana);
		push.responder(ResultadoDeEnvioPush.FALLO_PASAJERO);
		crearPendiente(ana, "Taller", enHoras(10));
		generar.ejecutar();

		entregar.ejecutar();
		assertThat(filaDelAviso(ana)).containsEntry("estado_envio", "pendiente").containsEntry("intentos", 1);
		entregar.ejecutar();
		assertThat(filaDelAviso(ana)).containsEntry("estado_envio", "pendiente").containsEntry("intentos", 2);
		entregar.ejecutar();
		assertThat(filaDelAviso(ana)).containsEntry("estado_envio", "fallida").containsEntry("intentos", 3);

		entregar.ejecutar();
		assertThat(enviosA(ana)).hasSize(3);
	}

	@Test
	void elAvisoDeUnEstudianteNoLlegaAlDispositivoDeOtro() throws Exception {
		Sesion ana = entrar("avisos.ana");
		Sesion beto = entrar("avisos.beto");
		conDispositivo(ana);
		conDispositivo(beto);
		crearPendiente(ana, "Solo de Ana", enHoras(10));

		generar.ejecutar();
		entregar.ejecutar();

		assertThat(enviosA(ana)).hasSize(1);
		assertThat(enviosA(beto)).isEmpty();
		avisos(beto).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	// --- Mis avisos ----------------------------------------------------------------------------------------

	@Test
	void marcarUnAvisoComoLeidoYDevolverloASinLeer() throws Exception {
		Sesion ana = entrar("avisos.leer");
		crearPendiente(ana, "Taller", enHoras(10));
		generar.ejecutar();
		int id = JsonPath.read(avisos(ana).andReturn().getResponse().getContentAsString(), "$.avisos[0].id");

		mvc.perform(patch("/api/mis/avisos/" + id).header("Authorization", "Bearer " + ana.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"leido\":true}"))
				.andExpect(status().isNoContent());
		avisos(ana).andExpect(jsonPath("$.sinLeer").value(0)).andExpect(jsonPath("$.avisos[0].leido").value(true));

		mvc.perform(patch("/api/mis/avisos/" + id).header("Authorization", "Bearer " + ana.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"leido\":false}"))
				.andExpect(status().isNoContent());
		avisos(ana).andExpect(jsonPath("$.sinLeer").value(1));
	}

	@Test
	void marcarTodosComoLeidosYListarSoloLosSinLeer() throws Exception {
		Sesion ana = entrar("avisos.todos");
		crearPendiente(ana, "Uno", enHoras(5));
		crearPendiente(ana, "Dos", enHoras(6));
		generar.ejecutar();
		avisos(ana).andExpect(jsonPath("$.sinLeer").value(2));

		mvc.perform(post("/api/mis/avisos/leidos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isNoContent());

		avisos(ana).andExpect(jsonPath("$.sinLeer").value(0)).andExpect(jsonPath("$.avisos.length()").value(2));
		mvc.perform(get("/api/mis/avisos?soloSinLeer=true").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void unAvisoAjenoRespondeComoSiNoExistiera() throws Exception {
		Sesion ana = entrar("avisos.duena");
		Sesion beto = entrar("avisos.intruso");
		crearPendiente(ana, "Taller", enHoras(10));
		generar.ejecutar();
		int id = JsonPath.read(avisos(ana).andReturn().getResponse().getContentAsString(), "$.avisos[0].id");

		mvc.perform(patch("/api/mis/avisos/" + id).header("Authorization", "Bearer " + beto.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"leido\":true}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos ese aviso")));
		avisos(ana).andExpect(jsonPath("$.sinLeer").value(1));
	}

	// --- Suscripción ---------------------------------------------------------------------------------------

	@Test
	void suscribirDevuelveElNombreDelDispositivoYSuscribirDeNuevoNoDuplica() throws Exception {
		Sesion ana = entrar("avisos.suscribir");

		suscribir(ana, suscripcionJson(ana.endpoint()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.consecutivo").value(1))
				.andExpect(jsonPath("$.nombre").value("Chrome en Android"))
				.andExpect(jsonPath("$.activo").value(true))
				.andExpect(jsonPath("$.endpoint").doesNotExist())
				.andExpect(jsonPath("$.keys").doesNotExist());
		suscribir(ana, suscripcionJson(ana.endpoint()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.consecutivo").value(1));

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dispositivo WHERE id_estudiante = ?", Integer.class, ana.id())).isEqualTo(1);
	}

	@Test
	void otroNavegadorDelMismoEstudianteEsOtroDispositivo() throws Exception {
		Sesion ana = entrar("avisos.dos.navegadores");

		suscribir(ana, suscripcionJson(ana.endpoint())).andExpect(jsonPath("$.consecutivo").value(1));
		suscribir(ana, suscripcionJson(ana.endpoint() + "-otro")).andExpect(jsonPath("$.consecutivo").value(2));
	}

	@Test
	void unaSuscripcionDeUnServicioDesconocidoOSinHttpsSeRechaza() throws Exception {
		Sesion ana = entrar("avisos.rechazo");

		suscribir(ana, suscripcionJson("https://atacante.example.com/avisos"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("todavía no admitimos")));
		suscribir(ana, suscripcionJson("http://fcm.googleapis.com/fcm/send/x")).andExpect(status().isUnprocessableEntity());
		suscribir(ana, suscripcionJson("https://169.254.169.254/latest/meta-data")).andExpect(status().isUnprocessableEntity());
		suscribir(ana, "{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/x\"}").andExpect(status().isBadRequest());
		suscribir(ana, "{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/x\",\"keys\":{\"p256dh\":\"abc\",\"auth\":\"abc\"}}")
				.andExpect(status().isUnprocessableEntity());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dispositivo WHERE id_estudiante = ?", Integer.class, ana.id())).isZero();
	}

	@Test
	void laLlavePublicaDiceSiHayWebPush() throws Exception {
		Sesion ana = entrar("avisos.llave");

		mvc.perform(get("/api/mis/dispositivos/llave-publica").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponible").value(true))
				.andExpect(jsonPath("$.llave").value("llave-publica-de-prueba"));

		push.estaDisponible(false);
		mvc.perform(get("/api/mis/dispositivos/llave-publica").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.disponible").value(false))
				.andExpect(jsonPath("$.llave").value(Matchers.nullValue()));
	}

	@Test
	void sinSesionNoSeAtiendeNada() throws Exception {
		mvc.perform(get("/api/mis/avisos")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/mis/avisos/leidos")).andExpect(status().isUnauthorized());
		mvc.perform(patch("/api/mis/avisos/1").contentType(MediaType.APPLICATION_JSON).content("{\"leido\":true}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/mis/dispositivos/llave-publica")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/mis/dispositivos/suscripcion").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}
}
