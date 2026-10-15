package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

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

/** El calendario con festivos de extremo a extremo, con seguridad real (RF07, SCRUM-85). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CalendarioIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	private String entrar(String correo) throws Exception {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true,
						Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		MvcResult login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	@Test
	void elDoceDeOctubreDe2026EsFestivoYElTreceEsLectivo() throws Exception {
		String token = entrar("calendario.festivo@ucundinamarca.edu.co");

		mvc.perform(get("/api/calendario/dias?desde=2026-10-11&hasta=2026-10-14").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dias.length()").value(4))
				.andExpect(jsonPath("$.dias[0].fecha").value("2026-10-11"))
				.andExpect(jsonPath("$.dias[0].dia").value("domingo"))
				.andExpect(jsonPath("$.dias[0].tipo").value("lectivo"))
				.andExpect(jsonPath("$.dias[1].fecha").value("2026-10-12"))
				.andExpect(jsonPath("$.dias[1].dia").value("lunes"))
				.andExpect(jsonPath("$.dias[1].tipo").value("festivo"))
				.andExpect(jsonPath("$.dias[1].festivo").value("Día de la Raza"))
				.andExpect(jsonPath("$.dias[2].tipo").value("lectivo"))
				.andExpect(jsonPath("$.dias[2].festivo").doesNotExist());
	}

	@Test
	void entreSemestresElDiaEstaFueraDelPeriodo() throws Exception {
		String token = entrar("calendario.receso@ucundinamarca.edu.co");

		mvc.perform(get("/api/calendario/dias?desde=2026-12-16&hasta=2026-12-16").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dias[0].tipo").value("fuera_de_periodo"));
	}

	@Test
	void sinRangoTraeLaFechaDeHoyDeBogotaYSieteDias() throws Exception {
		String token = entrar("calendario.hoy@ucundinamarca.edu.co");

		mvc.perform(get("/api/calendario/dias").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hoy").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
				.andExpect(jsonPath("$.dias.length()").value(7))
				.andExpect(jsonPath("$.dias[0].fecha").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}")));
	}

	@Test
	void unRangoAlRevesOMuyLargoSeRechaza() throws Exception {
		String token = entrar("calendario.rango@ucundinamarca.edu.co");

		mvc.perform(get("/api/calendario/dias?desde=2026-10-05&hasta=2026-10-01").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(get("/api/calendario/dias?desde=2026-01-01&hasta=2026-12-31").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void unaFechaMalEscritaEsUnaPeticionMalArmada() throws Exception {
		String token = entrar("calendario.formato@ucundinamarca.edu.co");

		mvc.perform(get("/api/calendario/dias?desde=12-10-2026").header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sinSesionNoSePuedeConsultar() throws Exception {
		mvc.perform(get("/api/calendario/dias")).andExpect(status().isUnauthorized());
	}
}
