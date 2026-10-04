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
import java.util.List;
import org.hamcrest.Matchers;
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
 * Definir cómo me evalúan (RF05, SCRUM-27).
 *
 * <p>Lo que hay que demostrar: la asignatura arranca con la plantilla 30/30/40 sin que nadie la
 * haya guardado, las dos reglas del 100 % se rechazan con un mensaje que dice cuánto falta, y una
 * actividad que mantiene su número de orden sobrevive a una edición de la estructura.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EstructuraDeEvaluacionIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String ALGEBRA = "CAD612021101";
	private static final String OTRA = "CAD612021106";

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

	/**
	 * Hoy la única forma de tener matrículas es importar el registro extendido. Cuando llegue el
	 * horario (SCRUM-25) aparecerán también las del período en curso y esta pantalla no cambia.
	 */
	private String conMatriculas(String correo) throws Exception {
		String token = entrar(correo);
		mvc.perform(put("/api/mis/perfil-academico")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombreArchivo":"registro_extendido.pdf","detectadas":2,"periodos":[
								  {"codigo":"2024-2","creditosMatriculados":6,"creditosAprobados":6,
								   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
								   "notas":[{"codigoAsignatura":"%s","nota":4.2},
								             {"codigoAsignatura":"%s","nota":4.0}]}]}"""
								.formatted(ALGEBRA, OTRA)))
				.andExpect(status().isCreated());
		return token;
	}

	private int idDe(String token, String codigoAsignatura) throws Exception {
		String lista = mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(lista,
				"$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(codigoAsignatura));
		return ids.getFirst();
	}

	private void guardar(String token, int idMatricula, String cuerpo, int esperado) throws Exception {
		mvc.perform(put("/api/mis/matriculas/" + idMatricula + "/evaluacion")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().is(esperado));
	}

	@Test
	void listaLasAsignaturasMatriculadasConElNombreQueTraeElPlan() throws Exception {
		String token = conMatriculas("evaluacion.lista@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].nombre").value(Matchers.not(Matchers.emptyString())))
				.andExpect(jsonPath("$[0].codigoPeriodo").value("2024-2"))
				.andExpect(jsonPath("$[0].tieneEstructura").value(false));
	}

	@Test
	void unaAsignaturaSinConfigurarLlegaConLaPlantillaDeTresCortes() throws Exception {
		String token = conMatriculas("evaluacion.plantilla@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				// Falso a propósito: es una propuesta, no algo que el estudiante haya confirmado.
				.andExpect(jsonPath("$.guardada").value(false))
				.andExpect(jsonPath("$.categorias.length()").value(3))
				.andExpect(jsonPath("$.categorias[0].nombre").value("Primer corte"))
				.andExpect(jsonPath("$.categorias[0].porcentaje").value(30.00))
				.andExpect(jsonPath("$.categorias[2].porcentaje").value(40.00))
				.andExpect(jsonPath("$.categorias[0].origen").value("plantilla"))
				.andExpect(jsonPath("$.categorias[0].actividades.length()").value(0));
	}

	@Test
	void laPropuestaDeLaPlantillaNoSeGuardaSola() throws Exception {
		String token = conMatriculas("evaluacion.nosegurada@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion")
				.header("Authorization", "Bearer " + token)).andExpect(status().isOk());

		mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$[?(@.codigoAsignatura == '%s')].categorias".formatted(ALGEBRA))
						.value(Matchers.contains(0)));
	}

	@Test
	void guardaElArbolQueElEstudianteArma() throws Exception {
		String token = conMatriculas("evaluacion.guarda@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);

		guardar(token, matricula, """
				{"categorias":[
				  {"consecutivo":1,"nombre":"Primer corte","porcentaje":30,"actividades":[
				    {"consecutivo":1,"nombre":"Parcial","porcentaje":70,"tipo":"parcial",
				     "fechaProgramada":"2026-10-20"},
				    {"consecutivo":2,"nombre":"Taller","porcentaje":30,"tipo":"taller"}]},
				  {"consecutivo":2,"nombre":"Segundo corte","porcentaje":30,"actividades":[]},
				  {"consecutivo":3,"nombre":"Final","porcentaje":40,"actividades":[]}]}""", 200);

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.guardada").value(true))
				.andExpect(jsonPath("$.categorias.length()").value(3))
				.andExpect(jsonPath("$.categorias[0].origen").value("estudiante"))
				.andExpect(jsonPath("$.categorias[0].actividades.length()").value(2))
				.andExpect(jsonPath("$.categorias[0].actividades[0].nombre").value("Parcial"))
				.andExpect(jsonPath("$.categorias[0].actividades[0].fechaProgramada").value("2026-10-20"))
				.andExpect(jsonPath("$.categorias[0].actividades[1].fechaProgramada").value(Matchers.nullValue()));
	}

	@Test
	void laListaDiceCualesAsignaturasYaEstanConfiguradas() throws Exception {
		String token = conMatriculas("evaluacion.cuales@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);
		guardar(token, matricula, """
				{"categorias":[{"consecutivo":1,"nombre":"Único","porcentaje":100,"actividades":[]}]}""", 200);

		mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$[?(@.idMatricula == %d)].tieneEstructura".formatted(matricula))
						.value(Matchers.contains(true)))
				.andExpect(jsonPath("$[?(@.codigoAsignatura == '%s')].tieneEstructura".formatted(OTRA))
						.value(Matchers.contains(false)));
	}

	@Test
	void rechazaCategoriasQueNoSuman100YDiceCuantoFalta() throws Exception {
		String token = conMatriculas("evaluacion.faltan@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);

		mvc.perform(put("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"categorias":[
								  {"consecutivo":1,"nombre":"Primer corte","porcentaje":30,"actividades":[]},
								  {"consecutivo":2,"nombre":"Segundo corte","porcentaje":30,"actividades":[]}]}"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("suman 60")))
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("faltan 40")));
	}

	@Test
	void rechazaActividadesQueNoSuman100YDiceEnCualCategoria() throws Exception {
		String token = conMatriculas("evaluacion.actividades@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);

		mvc.perform(put("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"categorias":[
								  {"consecutivo":1,"nombre":"Primer corte","porcentaje":100,"actividades":[
								    {"consecutivo":1,"nombre":"Parcial","porcentaje":50,"tipo":"parcial"},
								    {"consecutivo":2,"nombre":"Quiz","porcentaje":30,"tipo":"quiz"}]}]}"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("Primer corte")))
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("faltan 20")));
	}

	@Test
	void unaEstructuraRechazadaNoDejaRastro() throws Exception {
		// Si el rechazo guardara a medias, la asignatura quedaría con una suma imposible.
		String token = conMatriculas("evaluacion.sinrastro@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);

		guardar(token, matricula, """
				{"categorias":[{"consecutivo":1,"nombre":"Mitad","porcentaje":50,"actividades":[]}]}""", 422);

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.guardada").value(false));
	}

	@Test
	void quitarUnaActividadDejaLasQueSiguenConSuNumeroDeOrden() throws Exception {
		String token = conMatriculas("evaluacion.quitar@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);
		guardar(token, matricula, """
				{"categorias":[{"consecutivo":1,"nombre":"Único","porcentaje":100,"actividades":[
				  {"consecutivo":1,"nombre":"Parcial","porcentaje":40,"tipo":"parcial"},
				  {"consecutivo":2,"nombre":"Taller","porcentaje":30,"tipo":"taller"},
				  {"consecutivo":3,"nombre":"Quiz","porcentaje":30,"tipo":"quiz"}]}]}""", 200);

		guardar(token, matricula, """
				{"categorias":[{"consecutivo":1,"nombre":"Único","porcentaje":100,"actividades":[
				  {"consecutivo":1,"nombre":"Parcial","porcentaje":60,"tipo":"parcial"},
				  {"consecutivo":3,"nombre":"Quiz","porcentaje":40,"tipo":"quiz"}]}]}""", 200);

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.categorias[0].actividades.length()").value(2))
				.andExpect(jsonPath("$.categorias[0].actividades[1].consecutivo").value(3))
				.andExpect(jsonPath("$.categorias[0].actividades[1].nombre").value("Quiz"));
	}

	@Test
	void unaListaVaciaDevuelveLaAsignaturaAlEstadoSinConfigurar() throws Exception {
		String token = conMatriculas("evaluacion.vaciar@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);
		guardar(token, matricula, """
				{"categorias":[{"consecutivo":1,"nombre":"Único","porcentaje":100,"actividades":[]}]}""", 200);

		guardar(token, matricula, "{\"categorias\":[]}", 200);

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.guardada").value(false))
				.andExpect(jsonPath("$.categorias[0].origen").value("plantilla"));
	}

	@Test
	void unTipoDeActividadQueNoExisteEsUnDatoCorregible_noUnaFallaDelServidor() throws Exception {
		String token = conMatriculas("evaluacion.tipomalo@ucundinamarca.edu.co");
		int matricula = idDe(token, ALGEBRA);

		mvc.perform(put("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"categorias":[{"consecutivo":1,"nombre":"Único","porcentaje":100,"actividades":[
								  {"consecutivo":1,"nombre":"Algo","porcentaje":100,"tipo":"sustentacion"}]}]}"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("sustentacion")));
	}

	@Test
	void nadieVeNiConfiguraLaAsignaturaDeOtroEstudiante() throws Exception {
		String ajeno = conMatriculas("evaluacion.ajeno@ucundinamarca.edu.co");
		int deOtro = idDe(ajeno, ALGEBRA);
		String intruso = entrar("evaluacion.intruso@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/matriculas/" + deOtro + "/evaluacion")
						.header("Authorization", "Bearer " + intruso))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos")));

		guardar(intruso, deOtro, """
				{"categorias":[{"consecutivo":1,"nombre":"Único","porcentaje":100,"actividades":[]}]}""", 422);

		// Y la estructura del dueño sigue intacta.
		mvc.perform(get("/api/mis/matriculas/" + deOtro + "/evaluacion")
						.header("Authorization", "Bearer " + ajeno))
				.andExpect(jsonPath("$.guardada").value(false));
	}

	@Test
	void sinSesionNoSeVeNiSeGuardaNada() throws Exception {
		mvc.perform(get("/api/mis/matriculas")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/mis/matriculas/1/evaluacion")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/mis/matriculas/1/evaluacion")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorias\":[]}"))
				.andExpect(status().isUnauthorized());
	}
}
