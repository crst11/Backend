package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Configurar mis avisos (RF10, SCRUM-38), con seguridad real y PostgreSQL real: qué tipos recibo, con cuánta
 * anticipación y qué dispositivos reciben mis avisos.
 *
 * <p>Lo que hay que demostrar: lo que la persona apaga o cambia **rige** en la siguiente ronda de avisos, un
 * campo que falte no se lee como «apagado», y quitar un dispositivo lo deja sin avisos sin tocar los de otros.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConfigurarAvisosIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final ClavesDePushDePrueba.Suscripcion CLAVES = ClavesDePushDePrueba.nueva();

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

	private void crearPendiente(Sesion sesion, String titulo, long horas) throws Exception {
		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + sesion.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\":\"%s\",\"fechaLimite\":\"%s\"}".formatted(titulo, Instant.now().plus(horas, ChronoUnit.HOURS))))
				.andExpect(status().isCreated());
	}

	private ResultActions preferencias(Sesion sesion, String cuerpo) throws Exception {
		return mvc.perform(put("/api/mis/configuracion/avisos").header("Authorization", "Bearer " + sesion.token())
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private static String json(boolean activos, boolean vencimientos, boolean cambioDeRiesgo, int horas) {
		return "{\"activos\":%s,\"vencimientos\":%s,\"cambioDeRiesgo\":%s,\"anticipacionEnHoras\":%d}"
				.formatted(activos, vencimientos, cambioDeRiesgo, horas);
	}

	private ResultActions avisos(Sesion sesion) throws Exception {
		return mvc.perform(get("/api/mis/avisos").header("Authorization", "Bearer " + sesion.token()));
	}

	private void suscribir(Sesion sesion, String endpoint) throws Exception {
		mvc.perform(put("/api/mis/dispositivos/suscripcion").header("Authorization", "Bearer " + sesion.token())
						.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"endpoint\":\"%s\",\"keys\":{\"p256dh\":\"%s\",\"auth\":\"%s\"}}"
								.formatted(endpoint, CLAVES.clavePublica(), CLAVES.secretoEnBase64())))
				.andExpect(status().isOk());
	}

	// --- Preferencias ------------------------------------------------------------------------------------

	@Test
	void sinHaberlasCambiadoSonLasPredeterminadas() throws Exception {
		Sesion ana = entrar("config.defecto");

		mvc.perform(get("/api/mis/configuracion/avisos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activos").value(true))
				.andExpect(jsonPath("$.vencimientos").value(true))
				.andExpect(jsonPath("$.cambioDeRiesgo").value(true))
				.andExpect(jsonPath("$.anticipacionEnHoras").value(24));
	}

	@Test
	void lasPreferenciasQuedanGuardadasYSeLeenTalCual() throws Exception {
		Sesion ana = entrar("config.guardar");

		preferencias(ana, json(true, false, true, 72))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vencimientos").value(false))
				.andExpect(jsonPath("$.anticipacionEnHoras").value(72));

		mvc.perform(get("/api/mis/configuracion/avisos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.vencimientos").value(false))
				.andExpect(jsonPath("$.cambioDeRiesgo").value(true))
				.andExpect(jsonPath("$.anticipacionEnHoras").value(72));
	}

	@Test
	void apagarElAvisoDeVencimientoHaceQueNoSeGenere() throws Exception {
		Sesion ana = entrar("config.sin.vencimientos");
		preferencias(ana, json(true, false, true, 24)).andExpect(status().isOk());
		crearPendiente(ana, "Taller", 5);

		generar.ejecutar();

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void elInterruptorGeneralManda() throws Exception {
		Sesion ana = entrar("config.sin.avisos");
		preferencias(ana, json(false, true, true, 24)).andExpect(status().isOk());
		crearPendiente(ana, "Taller", 5);

		generar.ejecutar();

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void volverAEncenderElAvisoLoGeneraEnLaSiguienteRonda() throws Exception {
		Sesion ana = entrar("config.reencender");
		preferencias(ana, json(true, false, true, 24)).andExpect(status().isOk());
		crearPendiente(ana, "Taller", 5);
		generar.ejecutar();
		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));

		preferencias(ana, json(true, true, true, 24)).andExpect(status().isOk());
		generar.ejecutar();

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(1));
	}

	@Test
	void cambiarLaAnticipacionCambiaCuandoSeAvisa() throws Exception {
		Sesion ana = entrar("config.anticipacion");
		preferencias(ana, json(true, true, true, 2)).andExpect(status().isOk());
		crearPendiente(ana, "Dentro de cuarenta horas", 40);

		generar.ejecutar();
		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));

		preferencias(ana, json(true, true, true, 48)).andExpect(status().isOk());
		generar.ejecutar();

		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(1))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.containsString("«Dentro de cuarenta horas»")));
	}

	@Test
	void unaAnticipacionFueraDeRangoSeRechazaSinGuardarla() throws Exception {
		Sesion ana = entrar("config.rango");

		preferencias(ana, json(true, true, true, 0))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("entre 1 y 168 horas")));
		preferencias(ana, json(true, true, true, 169)).andExpect(status().isUnprocessableEntity());

		mvc.perform(get("/api/mis/configuracion/avisos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.anticipacionEnHoras").value(24));
	}

	@Test
	void unCampoQueFalteNoSeLeeComoApagado() throws Exception {
		Sesion ana = entrar("config.incompleta");

		preferencias(ana, "{\"activos\":true,\"anticipacionEnHoras\":24}").andExpect(status().isBadRequest());

		mvc.perform(get("/api/mis/configuracion/avisos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.vencimientos").value(true))
				.andExpect(jsonPath("$.cambioDeRiesgo").value(true));
	}

	@Test
	void laBaseDeDatosTambienGuardaElTope() {
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM information_schema.table_constraints WHERE constraint_name = 'ck_configuracion_anticipacion_maxima'",
				Integer.class)).isEqualTo(1);
	}

	// --- Dispositivos ------------------------------------------------------------------------------------

	@Test
	void veoLosDispositivosQueRecibenAvisosSinSusLlaves() throws Exception {
		Sesion ana = entrar("config.lista");
		suscribir(ana, ana.endpoint());

		mvc.perform(get("/api/mis/dispositivos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].consecutivo").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Chrome en Android"))
				.andExpect(jsonPath("$[0].activo").value(true))
				.andExpect(jsonPath("$[0].endpoint").doesNotExist())
				.andExpect(jsonPath("$[0].keys").doesNotExist());
	}

	@Test
	void quitarUnDispositivoLoSacaDeLaListaYLoDejaSinAvisos() throws Exception {
		Sesion ana = entrar("config.quitar");
		suscribir(ana, ana.endpoint());
		mvc.perform(delete("/api/mis/dispositivos/1").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/mis/dispositivos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.length()").value(0));
		crearPendiente(ana, "Taller", 5);
		generar.ejecutar();
		entregar.ejecutar();

		assertThat(push.envios().stream().filter(e -> e.suscripcion().endpoint().equals(ana.endpoint()))).isEmpty();
		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(1));
	}

	@Test
	void quitarUnoNoTocaLosDeLaMismaPersonaNiLosDeOtra() throws Exception {
		Sesion ana = entrar("config.dos.ana");
		Sesion beto = entrar("config.dos.beto");
		suscribir(ana, ana.endpoint());
		suscribir(ana, ana.endpoint() + "-segundo");
		suscribir(beto, beto.endpoint());

		mvc.perform(delete("/api/mis/dispositivos/1").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/mis/dispositivos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].consecutivo").value(2));
		mvc.perform(get("/api/mis/dispositivos").header("Authorization", "Bearer " + beto.token()))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void quitarUnDispositivoAjenoOQueNoExisteRespondeComoSiNoExistiera() throws Exception {
		Sesion ana = entrar("config.ajeno.ana");
		Sesion beto = entrar("config.ajeno.beto");
		suscribir(ana, ana.endpoint());

		mvc.perform(delete("/api/mis/dispositivos/1").header("Authorization", "Bearer " + beto.token()))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos ese dispositivo")));
		mvc.perform(delete("/api/mis/dispositivos/987").header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isUnprocessableEntity());

		mvc.perform(get("/api/mis/dispositivos").header("Authorization", "Bearer " + ana.token()))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void sinSesionNoSeAtiendeNada() throws Exception {
		mvc.perform(get("/api/mis/configuracion/avisos")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/mis/configuracion/avisos").contentType(MediaType.APPLICATION_JSON).content(json(true, true, true, 24)))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/mis/dispositivos")).andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/mis/dispositivos/1")).andExpect(status().isUnauthorized());
	}
}
