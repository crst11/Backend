package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort.IdentidadExterna;
import co.edu.ucundinamarca.cundiapp.domain.exception.IdentidadExternaInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SCRUM-48 de punta a punta con la seguridad real y PostgreSQL real. Lo único simulado es Google:
 * el verificador devuelve la identidad que tendría cada ID token (su propia prueba cubre la firma).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GoogleIntegracionTest {

	private static final String CLAVE = "claveSegura1";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private VerificadorDeIdentidadExternaPort google;

	private int crearCuentaActiva(String correo) {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true, Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		return creada.id();
	}

	private String accesoConContrasena(String correo) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	private void googleDice(String idToken, String sub, String correo) {
		given(google.verificar(idToken)).willReturn(new IdentidadExterna(sub, correo, true));
	}

	private MvcResult vincular(String acceso, String idToken) throws Exception {
		return mvc.perform(post("/api/mis/google")
						.header("Authorization", "Bearer " + acceso)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"idToken\":\"%s\"}".formatted(idToken)))
				.andReturn();
	}

	private MvcResult entrarConGoogle(String idToken) throws Exception {
		return mvc.perform(post("/api/publico/auth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"idToken\":\"%s\"}".formatted(idToken)))
				.andReturn();
	}

	@Test
	void vinculaGoogleYDespuesEntraConUnToqueConLaMismaSesionQueElLogin() throws Exception {
		int id = crearCuentaActiva("google.vincula@ucundinamarca.edu.co");
		String acceso = accesoConContrasena("google.vincula@ucundinamarca.edu.co");
		googleDice("token-ana", "sub-ana", "ana.diaz@gmail.com");

		mvc.perform(get("/api/mis/google").header("Authorization", "Bearer " + acceso))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vinculada").value(false));
		MvcResult vinculo = vincular(acceso, "token-ana");
		assertThat(vinculo.getResponse().getStatus()).isEqualTo(200);
		assertThat((String) JsonPath.read(vinculo.getResponse().getContentAsString(), "$.correo")).isEqualTo("ana.diaz@gmail.com");
		assertThat(vinculo.getResponse().getContentAsString()).doesNotContain("sub-ana");

		MvcResult sesion = entrarConGoogle("token-ana");

		assertThat(sesion.getResponse().getStatus()).isEqualTo(200);
		assertThat((String) JsonPath.read(sesion.getResponse().getContentAsString(), "$.cuenta.correo"))
				.isEqualTo("google.vincula@ucundinamarca.edu.co");
		assertThat(sesion.getResponse().getCookie("refresco").isHttpOnly()).isTrue();
		assertThat(sesion.getResponse().getCookie("XSRF-TOKEN")).isNotNull();
		assertThat(jdbc.queryForObject(
				"SELECT proveedor_origen FROM cundiapp.sesion WHERE id_estudiante = ? ORDER BY consec_sesion DESC LIMIT 1",
				String.class, id)).isEqualTo("google");
		assertThat(jdbc.queryForObject(
				"SELECT fecha_ultimo_acceso IS NOT NULL FROM cundiapp.credencial_acceso WHERE id_estudiante = ? AND proveedor = 'google'",
				Boolean.class, id)).isTrue();
	}

	@Test
	void unaCuentaDeGoogleSinVincularNoEntraYElMensajeDiceQueHacer() throws Exception {
		googleDice("token-desconocido", "sub-desconocido", "nadie@gmail.com");

		mvc.perform(post("/api/publico/auth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"idToken\":\"token-desconocido\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Google no vinculado"))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("vincúlala desde Mi cuenta")));
	}

	@Test
	void laMismaCuentaDeGoogleNoQuedaEnDosCuentas() throws Exception {
		crearCuentaActiva("google.primera@ucundinamarca.edu.co");
		crearCuentaActiva("google.segunda@ucundinamarca.edu.co");
		googleDice("token-compartido", "sub-compartido", "compartido@gmail.com");

		assertThat(vincular(accesoConContrasena("google.primera@ucundinamarca.edu.co"), "token-compartido")
				.getResponse().getStatus()).isEqualTo(200);
		MvcResult segunda = vincular(accesoConContrasena("google.segunda@ucundinamarca.edu.co"), "token-compartido");

		assertThat(segunda.getResponse().getStatus()).isEqualTo(409);
		assertThat((String) JsonPath.read(segunda.getResponse().getContentAsString(), "$.title")).isEqualTo("Google ya vinculado");
	}

	@Test
	void alDesvincularYaNoEntraConGooglePeroSiConSuContrasena() throws Exception {
		crearCuentaActiva("google.desvincula@ucundinamarca.edu.co");
		String acceso = accesoConContrasena("google.desvincula@ucundinamarca.edu.co");
		googleDice("token-quitar", "sub-quitar", "quitar@gmail.com");
		vincular(acceso, "token-quitar");

		mvc.perform(delete("/api/mis/google").header("Authorization", "Bearer " + acceso)).andExpect(status().isNoContent());

		assertThat(entrarConGoogle("token-quitar").getResponse().getStatus()).isEqualTo(404);
		mvc.perform(get("/api/mis/google").header("Authorization", "Bearer " + acceso))
				.andExpect(jsonPath("$.vinculada").value(false));
		accesoConContrasena("google.desvincula@ucundinamarca.edu.co");
	}

	@Test
	void unTokenDeGoogleInvalidoOVacioNoPasaYVincularExigeSesion() throws Exception {
		given(google.verificar("token-falso")).willThrow(new IdentidadExternaInvalidaException());

		assertThat(entrarConGoogle("token-falso").getResponse().getStatus()).isEqualTo(401);
		assertThat(entrarConGoogle("").getResponse().getStatus()).isEqualTo(400);
		mvc.perform(post("/api/mis/google").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"x\"}"))
				.andExpect(status().isUnauthorized());
	}
}
