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
		String correo = "sesiones.repetida@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		String token = entrar(correo, "Chrome en Windows");
		MvcResult abiertas = mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andReturn();
		int consecutivo = ((List<Integer>) JsonPath.read(abiertas.getResponse().getContentAsString(),
				"$[*].consecutivo")).get(0);

		mvc.perform(delete("/api/mis/sesiones/" + consecutivo).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mvc.perform(delete("/api/mis/sesiones/" + consecutivo).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void cerrarTodasDejaLaListaVacia() throws Exception {
		String correo = "sesiones.todas@ucundinamarca.edu.co";
		crearCuentaActiva(correo);
		entrar(correo, "Chrome en Windows");
		String token = entrar(correo, "Mi teléfono");

		mvc.perform(delete("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		// El token de acceso sigue valiendo hasta vencer (es sin estado), pero ya no queda sesión abierta.
		mvc.perform(get("/api/mis/sesiones").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void sinTokenNoSeVenLasSesiones() throws Exception {
		mvc.perform(get("/api/mis/sesiones")).andExpect(status().isUnauthorized());
	}
}
