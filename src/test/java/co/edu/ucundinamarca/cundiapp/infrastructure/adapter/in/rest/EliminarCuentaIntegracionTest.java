package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SCRUM-64 con la seguridad real y PostgreSQL real: eliminar la cuenta la deja inactiva (no la
 * borra), revoca sus sesiones y bloquea cualquier inicio de sesión posterior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EliminarCuentaIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	private int crearCuentaActiva(String correo) {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true, Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		return creada.id();
	}

	private record Credenciales(String acceso, String refresco, String csrf) {
	}

	private Credenciales loginExitoso(String correo) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andReturn();
		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
		return new Credenciales(
				JsonPath.read(resultado.getResponse().getContentAsString(), "$.tokenDeAcceso"),
				resultado.getResponse().getCookie("refresco").getValue(),
				resultado.getResponse().getCookie("XSRF-TOKEN").getValue());
	}

	@Test
	void eliminaLaCuentaYYaNoPuedeVolverAIniciarSesion() throws Exception {
		crearCuentaActiva("eliminar.cuenta@ucundinamarca.edu.co");
		Credenciales credenciales = loginExitoso("eliminar.cuenta@ucundinamarca.edu.co");

		mvc.perform(delete("/api/mis/cuenta").header("Authorization", "Bearer " + credenciales.acceso()))
				.andExpect(status().isNoContent());

		MvcResult segundoLogin = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"eliminar.cuenta@ucundinamarca.edu.co\",\"contrasena\":\"%s\"}".formatted(CLAVE)))
				.andReturn();
		assertThat(segundoLogin.getResponse().getStatus()).isEqualTo(403);
		assertThat(segundoLogin.getResponse().getContentAsString()).contains("inactiva");
	}

	@Test
	void eliminarLaCuentaRevocaElRefrescoVigente() throws Exception {
		crearCuentaActiva("eliminar.refresco@ucundinamarca.edu.co");
		Credenciales credenciales = loginExitoso("eliminar.refresco@ucundinamarca.edu.co");

		mvc.perform(delete("/api/mis/cuenta").header("Authorization", "Bearer " + credenciales.acceso()))
				.andExpect(status().isNoContent());

		mvc.perform(post("/api/publico/auth/refresco")
						.cookie(new Cookie("refresco", credenciales.refresco()), new Cookie("XSRF-TOKEN", credenciales.csrf()))
						.header("X-XSRF-TOKEN", credenciales.csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinTokenNoSePuedeEliminarLaCuenta() throws Exception {
		mvc.perform(delete("/api/mis/cuenta")).andExpect(status().isUnauthorized());
	}

	@Test
	void despuesDeEliminarLaCuentaElTokenYaNoSirveParaNada() throws Exception {
		crearCuentaActiva("eliminar.dosveces@ucundinamarca.edu.co");
		Credenciales credenciales = loginExitoso("eliminar.dosveces@ucundinamarca.edu.co");
		mvc.perform(delete("/api/mis/cuenta").header("Authorization", "Bearer " + credenciales.acceso()))
				.andExpect(status().isNoContent());

		// Eliminar la cuenta revoca sus sesiones, y desde SCRUM-77 eso corta el token de inmediato.
		// Antes el token seguía sirviendo unos minutos y la segunda llamada llegaba a responder 422.
		mvc.perform(delete("/api/mis/cuenta").header("Authorization", "Bearer " + credenciales.acceso()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void trasEliminarLaCuentaSePuedeVolverARegistrarConElMismoCorreo() throws Exception {
		String correo = "eliminar.reregistro@ucundinamarca.edu.co";
		int idOriginal = crearCuentaActiva(correo);
		Credenciales credenciales = loginExitoso(correo);
		mvc.perform(delete("/api/mis/cuenta").header("Authorization", "Bearer " + credenciales.acceso()))
				.andExpect(status().isNoContent());

		MvcResult registro = mvc.perform(post("/api/publico/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"%s","contrasena":"OtraClaveNueva1!","nombres":"Ana Nueva","apellidos":"Díaz",
								 "aceptaTratamientoDatos":true}"""
								.formatted(correo)))
				.andExpect(status().isCreated())
				.andReturn();
		assertThat((String) JsonPath.read(registro.getResponse().getContentAsString(), "$.estado")).isEqualTo("pendiente");

		// Es la misma cuenta de siempre (mismo id), no una duplicada, y la contraseña vieja ya no sirve.
		var recreada = estudiantes.buscarPorCorreo(new CorreoInstitucional(correo)).orElseThrow();
		assertThat(recreada.id()).isEqualTo(idOriginal);
		assertThat(recreada.nombres()).isEqualTo("Ana Nueva");
		MvcResult loginConClaveVieja = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andReturn();
		assertThat(loginConClaveVieja.getResponse().getStatus()).isEqualTo(401);
	}
}
