package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * La nota que necesito, el simulador y la meta (RF08, SCRUM-33 y SCRUM-34), con seguridad real y
 * PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: la nota requerida sale del dominio con el peso real de cada
 * actividad, simular no deja nada guardado, la meta se guarda y la de la consulta no la cambia, y
 * una asignatura ajena o sin estructura se rechaza con un mensaje útil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProyeccionYSimuladorIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String ALGEBRA = "CAD612021101";

	/**
	 * Primer corte 30 % (Parcial 70 %, Taller 30 %), segundo corte 30 % (Proyecto 100 %) y tercer
	 * corte 40 % sin desglosar.
	 */
	private static final String ESTRUCTURA = """
			{"categorias":[
			  {"consecutivo":1,"nombre":"Primer corte","porcentaje":30,"origen":"estudiante","actividades":[
			    {"consecutivo":1,"nombre":"Parcial","porcentaje":70,"tipo":"parcial"},
			    {"consecutivo":2,"nombre":"Taller","porcentaje":30,"tipo":"taller"}]},
			  {"consecutivo":2,"nombre":"Segundo corte","porcentaje":30,"origen":"estudiante","actividades":[
			    {"consecutivo":1,"nombre":"Proyecto","porcentaje":100,"tipo":"taller"}]},
			  {"consecutivo":3,"nombre":"Tercer corte","porcentaje":40,"origen":"estudiante","actividades":[]}]}""";

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

	/** Una asignatura matriculada, que hoy solo se consigue importando el registro extendido. */
	private String conMatricula(String correo) throws Exception {
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
								{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
								  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":3,
								   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
								   "notas":[{"codigoAsignatura":"%s","nota":4.2}]}]}""".formatted(ALGEBRA)))
				.andExpect(status().isCreated());
		return token;
	}

	private int matriculaDe(String token) throws Exception {
		String lista = mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(lista, "$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(ALGEBRA));
		return ids.getFirst();
	}

	private void definirEstructura(String token, int matricula) throws Exception {
		mvc.perform(put("/api/mis/matriculas/" + matricula + "/evaluacion")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(ESTRUCTURA))
				.andExpect(status().isOk());
	}

	private void anotar(String token, int matricula, int categoria, int actividad, String nota) throws Exception {
		mvc.perform(put("/api/mis/matriculas/%d/evaluacion/categorias/%d/actividades/%d/calificacion"
						.formatted(matricula, categoria, actividad))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nota\":" + nota + "}"))
				.andExpect(status().isOk());
	}

	private ResultActions proyeccion(String token, int matricula, String consulta) throws Exception {
		return mvc.perform(get("/api/mis/matriculas/" + matricula + "/proyeccion" + consulta)
				.header("Authorization", "Bearer " + token));
	}

	private ResultActions simular(String token, int matricula, String cuerpo) throws Exception {
		return mvc.perform(post("/api/mis/matriculas/" + matricula + "/simulaciones")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	@Test
	void sinNotasHaceFaltaLaMetaEntera() throws Exception {
		String token = conMatricula("proy.sinnotas@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		proyeccion(token, matricula, "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacion").value("alcanzable"))
				.andExpect(jsonPath("$.meta").value(3.0))
				.andExpect(jsonPath("$.notaRequerida").value(3.0))
				.andExpect(jsonPath("$.maximoPosible").value(5.0))
				.andExpect(jsonPath("$.asignatura.codigoAsignatura").value(ALGEBRA));
	}

	@Test
	void conUnaNotaRealLaRequeridaSeRedondeaHaciaArriba() throws Exception {
		String token = conMatricula("proy.parcial@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);
		anotar(token, matricula, 1, 1, "4.0");

		// Parcial 4.0: 0.84 ganado y 21 % evaluado. Falta (3.0 - 0.84) / 0.79 = 2.7342...
		proyeccion(token, matricula, "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puntos").value(0.84))
				.andExpect(jsonPath("$.porcentajeEvaluado").value(21.0))
				.andExpect(jsonPath("$.notaRequerida").value(2.74));
	}

	@Test
	void simularNoGuardaNada() throws Exception {
		String token = conMatricula("proy.simular@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		simular(token, matricula, """
				{"notas":[{"consecCategoria":1,"consecActividad":1,"nota":4.0},
				          {"consecCategoria":1,"consecActividad":2,"nota":3.0}]}""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puntos").value(1.11))
				.andExpect(jsonPath("$.notaRequerida").value(2.7));

		// Lo hipotético no existe: la proyección real sigue como estaba y la actividad no tiene nota.
		proyeccion(token, matricula, "")
				.andExpect(jsonPath("$.puntos").value(0.0))
				.andExpect(jsonPath("$.notaRequerida").value(3.0));
		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.categorias[0].actividades[0].nota").value(Matchers.nullValue()));
	}

	@Test
	void cuandoLaMetaYaNoSeAlcanzaNoSeInventaUnaNota() throws Exception {
		String token = conMatricula("proy.inalcanzable@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);
		anotar(token, matricula, 1, 1, "0.0");
		anotar(token, matricula, 1, 2, "0.0");
		anotar(token, matricula, 2, 1, "0.0");

		// Queda 40 % por evaluar: con 5.0 en todo se llega a 2.00, y la meta es 4.5.
		proyeccion(token, matricula, "?meta=4.5")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacion").value("inalcanzable"))
				.andExpect(jsonPath("$.notaRequerida").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.maximoPosible").value(2.0));
	}

	@Test
	void laMetaSeGuardaYLaDeLaConsultaNoLaCambia() throws Exception {
		String token = conMatricula("proy.meta@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		mvc.perform(get("/api/mis/configuracion").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta").value(3.0));

		mvc.perform(patch("/api/mis/configuracion")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"meta\":4.0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta").value(4.0));

		proyeccion(token, matricula, "").andExpect(jsonPath("$.notaRequerida").value(4.0));
		proyeccion(token, matricula, "?meta=3.5").andExpect(jsonPath("$.notaRequerida").value(3.5));
		simular(token, matricula, "{\"meta\":2.5,\"notas\":[]}").andExpect(jsonPath("$.notaRequerida").value(2.5));

		mvc.perform(get("/api/mis/configuracion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.meta").value(4.0));
	}

	@Test
	void unaMetaFueraDeLaEscalaSeRechazaSinGuardarla() throws Exception {
		String token = entrar("proy.escala@ucundinamarca.edu.co");

		mvc.perform(patch("/api/mis/configuracion")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"meta\":5.5}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("0.0 y 5.0")));

		mvc.perform(get("/api/mis/configuracion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.meta").value(3.0));
	}

	@Test
	void sinEstructuraDeEvaluacionNoSeProyectaYSeDiceQueHaceFalta() throws Exception {
		String token = conMatricula("proy.sinestructura@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);

		proyeccion(token, matricula, "")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("Define primero cómo te evalúan")));
	}

	@Test
	void unaActividadQueNoExisteOUnaNotaMalEscritaSeRechazan() throws Exception {
		String token = conMatricula("proy.invalida@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		simular(token, matricula, "{\"notas\":[{\"consecCategoria\":1,\"consecActividad\":9,\"nota\":4.0}]}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("no está en la estructura")));
		simular(token, matricula, "{\"notas\":[{\"consecCategoria\":1,\"consecActividad\":1,\"nota\":7.0}]}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("0.0 y 5.0")));
	}

	@Test
	void unaAsignaturaAjenaRespondeComoSiNoExistiera() throws Exception {
		String dueno = conMatricula("proy.dueno@ucundinamarca.edu.co");
		int matricula = matriculaDe(dueno);
		definirEstructura(dueno, matricula);
		String otro = entrar("proy.otro@ucundinamarca.edu.co");

		proyeccion(otro, matricula, "")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos esa asignatura")));
		simular(otro, matricula, "{\"notas\":[]}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos esa asignatura")));
	}

	@Test
	void sinSesionNoSeAtiende() throws Exception {
		mvc.perform(get("/api/mis/matriculas/1/proyeccion")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/mis/matriculas/1/simulaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/mis/configuracion")).andExpect(status().isUnauthorized());
	}
}
