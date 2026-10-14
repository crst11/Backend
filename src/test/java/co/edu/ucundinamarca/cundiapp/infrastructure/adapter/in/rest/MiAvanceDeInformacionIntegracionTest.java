package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

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

/** El avance de información de extremo a extremo, con seguridad real y PostgreSQL real (SCRUM-84). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MiAvanceDeInformacionIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String RUTA = "/api/mis/avance-de-informacion";

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

	private void elegirPrograma(String token) throws Exception {
		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
	}

	private void importarHorario(String token) throws Exception {
		mvc.perform(post("/api/mis/importaciones/horario").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"periodo":"2026-2","asignaturas":[
								  {"codigo":"CAD612021519","grupo":"G1","clases":[
								    {"dia":"martes","horaInicio":"09:00","horaFin":"11:00"}]},
								  {"codigo":"CAD612021520","grupo":"G1","clases":[
								    {"dia":"miercoles","horaInicio":"07:00","horaFin":"09:00"}]}]}"""))
				.andExpect(status().isCreated());
	}

	private int idDe(String token, String codigo) throws Exception {
		String lista = mvc.perform(get("/api/mis/matriculas?semestre=actual").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		java.util.List<Integer> ids = JsonPath.read(lista, "$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(codigo));
		return ids.getFirst();
	}

	@Test
	void unEstudianteNuevoTieneCeroPorcientoYLosDosReportesPorSubir() throws Exception {
		String token = entrar("avance.nuevo@ucundinamarca.edu.co");

		mvc.perform(get(RUTA).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.porcentaje").value(0))
				.andExpect(jsonPath("$.hechos").value(0))
				.andExpect(jsonPath("$.total").value(2))
				.andExpect(jsonPath("$.pasos[0].tipo").value("registro_extendido"))
				.andExpect(jsonPath("$.pasos[0].hecho").value(false))
				.andExpect(jsonPath("$.pasos[1].tipo").value("horario"));
	}

	@Test
	void alImportarElHorarioCuentaElReporteYAparecenSusAsignaturasPorCompletar() throws Exception {
		String token = entrar("avance.horario@ucundinamarca.edu.co");
		elegirPrograma(token);
		importarHorario(token);

		mvc.perform(get(RUTA).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(4))
				.andExpect(jsonPath("$.hechos").value(1))
				.andExpect(jsonPath("$.porcentaje").value(25))
				.andExpect(jsonPath("$.pasos[1].hecho").value(true))
				.andExpect(jsonPath("$.pasos[2].tipo").value("asignatura"))
				.andExpect(jsonPath("$.pasos[2].hecho").value(false))
				.andExpect(jsonPath("$.pasos[2].idMatricula").isNumber());
	}

	@Test
	void unaAsignaturaConActividadesGuardadasSeCuentaComoHecha() throws Exception {
		String token = entrar("avance.actividades@ucundinamarca.edu.co");
		elegirPrograma(token);
		importarHorario(token);
		int matricula = idDe(token, "CAD612021519");
		mvc.perform(put("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"categorias":[{"consecutivo":1,"nombre":"R1","porcentaje":100,"actividades":[
								  {"consecutivo":1,"nombre":"Taller 1","porcentaje":100}]}]}"""))
				.andExpect(status().isOk());

		mvc.perform(get(RUTA).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hechos").value(2))
				.andExpect(jsonPath("$.porcentaje").value(50))
				.andExpect(jsonPath("$.pasos[?(@.idMatricula == %d)].hecho".formatted(matricula)).value(true));
	}

	@Test
	void unRegistroExtendidoConfirmadoCuentaElPrimerPaso() throws Exception {
		String token = entrar("avance.registro@ucundinamarca.edu.co");
		elegirPrograma(token);
		mvc.perform(post("/api/mis/importaciones").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
								  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":3,
								   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
								   "notas":[{"codigoAsignatura":"CAD612021101","nota":4.2}]}]}"""))
				.andExpect(status().isCreated());

		mvc.perform(get(RUTA).header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.pasos[0].hecho").value(true))
				.andExpect(jsonPath("$.porcentaje").value(50));
	}

	@Test
	void elAvanceEsDeCadaEstudiante() throws Exception {
		String uno = entrar("avance.uno@ucundinamarca.edu.co");
		elegirPrograma(uno);
		importarHorario(uno);
		String otro = entrar("avance.otro@ucundinamarca.edu.co");

		mvc.perform(get(RUTA).header("Authorization", "Bearer " + otro))
				.andExpect(jsonPath("$.total").value(2))
				.andExpect(jsonPath("$.hechos").value(0));
	}

	@Test
	void sinSesionNoSeVe() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
	}
}
