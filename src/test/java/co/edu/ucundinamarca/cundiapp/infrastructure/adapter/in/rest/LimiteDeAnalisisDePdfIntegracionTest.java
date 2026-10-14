package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing.ReporteDeHorarioDeEjemplo;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** El análisis de PDF tiene un cupo por estudiante, con 429 y cuánto esperar (SCRUM-88). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LimiteDeAnalisisDePdfIntegracionTest {

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
		var login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	private static MockMultipartFile reporte() {
		return new MockMultipartFile(
				"archivo", "horario.pdf", MediaType.APPLICATION_PDF_VALUE, ReporteDeHorarioDeEjemplo.pdf());
	}

	@Test
	void despuesDeDiezAnalisisElOncenoPideEsperarYDiceCuanto() throws Exception {
		String token = entrar("limite.pdf@ucundinamarca.edu.co");
		// Sin programa elegido cada análisis responde 422, pero cuenta igual: lo caro es abrir el PDF.
		for (int i = 0; i < 10; i++) {
			mvc.perform(multipart("/api/mis/importaciones/horario/analisis").file(reporte())
							.header("Authorization", "Bearer " + token))
					.andExpect(status().isUnprocessableContent());
		}

		mvc.perform(multipart("/api/mis/importaciones/horario/analisis").file(reporte())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.detail").value(containsString("Espera")));
	}

	@Test
	void elCupoEsDeCadaEstudianteYNoDeTodos() throws Exception {
		String uno = entrar("limite.uno@ucundinamarca.edu.co");
		for (int i = 0; i < 11; i++) {
			mvc.perform(multipart("/api/mis/importaciones/analisis").file(reporte()).header("Authorization", "Bearer " + uno));
		}
		String otro = entrar("limite.otro@ucundinamarca.edu.co");

		mvc.perform(multipart("/api/mis/importaciones/horario/analisis").file(reporte())
						.header("Authorization", "Bearer " + otro))
				.andExpect(status().isUnprocessableContent());
	}
}
