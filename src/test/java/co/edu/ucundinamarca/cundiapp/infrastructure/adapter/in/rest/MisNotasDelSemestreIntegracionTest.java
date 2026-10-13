package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/**
 * *Mis notas* solo ofrece las asignaturas del semestre actual (SCRUM-86), con seguridad real y
 * PostgreSQL real. Las de semestres anteriores siguen en el historial.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MisNotasDelSemestreIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";

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

	/** Una asignatura aprobada de un semestre anterior y otra que cursa ahora, importada del horario. */
	private String conUnSemestreAnteriorYUnoActual(String correo) throws Exception {
		String token = entrar(correo);
		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
								  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":3,
								   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
								   "notas":[{"codigoAsignatura":"CAD612021101","nota":4.2}]}]}"""))
				.andExpect(status().isCreated());
		mvc.perform(post("/api/mis/importaciones/horario").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"periodo":"2026-2","asignaturas":[
								  {"codigo":"CAD612021519","grupo":"SIS.FUSA 501M","clases":[]},
								  {"codigo":"CAD612021520","grupo":"SIS.FUSA 501M","clases":[]}]}"""))
				.andExpect(status().isCreated());
		return token;
	}

	@Test
	void conSemestreActualSoloTraeLasQueEstaCursando() throws Exception {
		String token = conUnSemestreAnteriorYUnoActual("notas.semestre@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/matriculas?semestre=actual").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[*].codigoPeriodo").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("2026-2"))))
				.andExpect(jsonPath("$[*].enCurso").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
	}

	@Test
	void sinCriterioSigueTrayendoTodasPorqueElHorarioYElHistorialLasNecesitan() throws Exception {
		String token = conUnSemestreAnteriorYUnoActual("notas.todas@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void sinAsignaturasEnCursoLaListaVieneVaciaNoConError() throws Exception {
		String token = entrar("notas.vacio@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/matriculas?semestre=actual").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unSemestreQueNoEsActualSeRechazaDiciendoCualesHay() throws Exception {
		String token = entrar("notas.invalido@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/matriculas?semestre=2024-1").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(containsString("actual")));
	}

	@Test
	void sinSesionNoSeVe() throws Exception {
		mvc.perform(get("/api/mis/matriculas?semestre=actual")).andExpect(status().isUnauthorized());
	}
}
