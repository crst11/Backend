package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Ver y cerrar sesiones abiertas con la seguridad real y PostgreSQL real (SCRUM-49). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MisSesionesIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	private void crearCuentaActiva(String correo) {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true,
						Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
	}

	private String entrar(String correo, String agente) throws Exception {
		MvcResult login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.header("User-Agent", agente)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	@Test
	void muestraCadaDispositivoConSesionAbierta() throws Exception {
		String correo = "sesiones.lista@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Chrome en Windows");
		String token = entrar(correo, "CundiApp en Android");

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				// La más reciente va primero.
				.andExpect(jsonPath("$[0].dispositivo").value("CundiApp en Android"))
				.andExpect(jsonPath("$[1].dispositivo").value("Chrome en Windows"))
				.andExpect(jsonPath("$[0].metodo").value("local"));
	}

	@Test
	void entrarDosVecesEnElMismoNavegadorNoAgregaUnaLineaMas() throws Exception {
		// SCRUM-73. Antes cada entrada —y cada renovación del token— dejaba su propia fila.
		String correo = "sesiones.mismodispositivo@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Chrome en Windows");
		entrar(correo, "Chrome en Windows");
		String token = entrar(correo, "Chrome en Windows");

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].dispositivo").value("Chrome en Windows"));
	}

	@Test
	void marcaElDispositivoDesdeElQueSeEstaMirando() throws Exception {
		String correo = "sesiones.actual@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Chrome en Windows");
		String token = entrar(correo, "CundiApp en Android");

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$[0].dispositivo").value("CundiApp en Android"))
				.andExpect(jsonPath("$[0].esLaActual").value(true))
				.andExpect(jsonPath("$[1].esLaActual").value(false));
	}

	@Test
	void elDispositivoDiceDesdeCuandoEstaDentroYCuandoFueSuUltimoAcceso() throws Exception {
		String correo = "sesiones.fechas@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Chrome en Windows");
		String token = entrar(correo, "Chrome en Windows");

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$[0].primerAcceso").exists())
				.andExpect(jsonPath("$[0].ultimoAcceso").exists());
	}

	@Test
	void cerrarTodasCortaElAccesoDeInmediatoYNoEnVeinteMinutos() throws Exception {
		// SCRUM-77. Antes el token de acceso seguía sirviendo hasta que vencía, porque el JWT es sin
		// estado y nadie comprobaba su sesión. Quien cierra todas suele sospechar que alguien entró:
		// dejarle el acceso veinte minutos más es justo lo que el botón promete evitar.
		String correo = "sesiones.corteinmediato@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		String token = entrar(correo, "Chrome en Windows");
		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mvc.perform(delete("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cerrarUnaSesionCortaElAccesoDeEseTokenYNoElDeLosDemas() throws Exception {
		String correo = "sesiones.corteunasola@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		String elQueSeCierra = entrar(correo, "Equipo prestado");
		String elQueSigue = entrar(correo, "Mi teléfono");

		MvcResult abiertas = mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + elQueSigue))
				.andReturn();
		java.util.List<Integer> delPrestado = JsonPath.read(abiertas.getResponse().getContentAsString(),
				"$[?(@.dispositivo == 'Equipo prestado')].consecutivo");

		mvc.perform(delete("/api/mis/sesiones/" + delPrestado.getFirst())
				.header("Authorization", "Bearer " + elQueSigue)).andExpect(status().isNoContent());

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + elQueSeCierra))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + elQueSigue))
				.andExpect(status().isOk());
	}

	@Test
	void cerrarUnDispositivoCierraTodasSusSesiones() throws Exception {
		// Revocar solo la última de la cadena dejaría el dispositivo dentro con las anteriores.
		String correo = "sesiones.cierracadena@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Equipo prestado");
		entrar(correo, "Equipo prestado");
		String token = entrar(correo, "Mi teléfono");

		MvcResult abiertas = mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.length()").value(2))
				.andReturn();
		java.util.List<Integer> consecutivos = JsonPath.read(abiertas.getResponse().getContentAsString(),
				"$[?(@.dispositivo == 'Equipo prestado')].consecutivo");
		int prestado = consecutivos.getFirst();

		mvc.perform(delete("/api/mis/sesiones/" + prestado).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].dispositivo").value("Mi teléfono"));
	}

	@Test
	void noDevuelveLaHuellaDelTokenDeRefresco() throws Exception {
		String correo = "sesiones.huella@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		String token = entrar(correo, "Chrome en Windows");

		MvcResult resultado = mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(resultado.getResponse().getContentAsString()).doesNotContain("huella", "refresco");
	}

	@Test
	void cerrarUnaSesionDejaLaOtraAbiertaYSuRefrescoDejaDeServir() throws Exception {
		String correo = "sesiones.cerrar.una@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		MvcResult primera = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.header("User-Agent", "Equipo prestado")
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		Cookie refrescoDeLaPrimera = primera.getResponse().getCookie("refresco");
		Cookie csrfDeLaPrimera = primera.getResponse().getCookie("XSRF-TOKEN");
		String token = entrar(correo, "Mi teléfono");

		MvcResult abiertas = mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andReturn();
		List<Integer> consecutivos = JsonPath.read(abiertas.getResponse().getContentAsString(), "$[*].consecutivo");
		int laPrestada = consecutivos.get(1);

		mvc.perform(delete("/api/mis/sesiones/" + laPrestada).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].dispositivo").value("Mi teléfono"));

		// La sesión cerrada deja de servir de inmediato: su token de refresco ya no renueva.
		mvc.perform(post("/api/publico/auth/refresco")
						.cookie(refrescoDeLaPrimera, csrfDeLaPrimera)
						.header("X-XSRF-TOKEN", csrfDeLaPrimera.getValue()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cerrarUnaQueYaEstabaCerradaResponde422() throws Exception {
		// Se cierra la de OTRO dispositivo: desde SCRUM-77, cerrar la propia deja el token sin valer,
		// y entonces el segundo intento sería 401 por sesión cerrada y no 422 por la regla de negocio.
		String correo = "sesiones.repetida@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Equipo prestado");
		String token = entrar(correo, "Mi teléfono");
		MvcResult abiertas = mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andReturn();
		List<Integer> delPrestado = JsonPath.read(abiertas.getResponse().getContentAsString(),
				"$[?(@.dispositivo == 'Equipo prestado')].consecutivo");

		mvc.perform(delete("/api/mis/sesiones/" + delPrestado.getFirst()).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mvc.perform(delete("/api/mis/sesiones/" + delPrestado.getFirst()).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void cerrarTodasDejaFueraTambienAQuienLoPidio() throws Exception {
		String correo = "sesiones.todas@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Chrome en Windows");
		String token = entrar(correo, "Mi teléfono");

		mvc.perform(delete("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		// Antes el token seguía valiendo hasta vencer y la lista salía vacía; ahora queda fuera ya.
		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinTokenNoSeVenLasSesiones() throws Exception {
		mvc.perform(get("/api/mis/sesiones")).andExpect(status().isUnauthorized());
	}
}
