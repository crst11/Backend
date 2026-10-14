package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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

/** Los dos análisis de PDF rechazan un archivo que no es un PDF aunque se llame `.pdf` (SCRUM-89). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PdfSubidoIntegracionTest {

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

	private static MockMultipartFile txtConNombreDePdf() {
		return new MockMultipartFile(
				"archivo", "reporte.pdf", MediaType.APPLICATION_PDF_VALUE, "esto no es un pdf".getBytes(StandardCharsets.US_ASCII));
	}

	@Test
	void elRegistroExtendidoRechazaUnTxtRenombradoAPdf() throws Exception {
		String token = entrar("pdf.registro@ucundinamarca.edu.co");

		mvc.perform(multipart("/api/mis/importaciones/analisis").file(txtConNombreDePdf())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("archivo PDF que descargaste")));
	}

	@Test
	void elReporteDeHorarioRechazaUnTxtRenombradoAPdf() throws Exception {
		String token = entrar("pdf.horario@ucundinamarca.edu.co");

		mvc.perform(multipart("/api/mis/importaciones/horario/analisis").file(txtConNombreDePdf())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("archivo PDF que descargaste")));
	}
}
