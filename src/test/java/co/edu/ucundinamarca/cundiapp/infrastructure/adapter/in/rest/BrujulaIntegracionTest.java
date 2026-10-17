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
 * La Brújula Académica y sus umbrales (RF09, SCRUM-35 y SCRUM-36), con seguridad real y PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: el nivel sale de la nota que hace falta con los umbrales del estudiante,
 * explica por qué, va de lo más urgente a lo menos, y cambiar los umbrales se refleja en la siguiente
 * consulta sin otro paso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BrujulaIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String REDES = "CAD612021519";

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

	/** Dos asignaturas que cursa ahora, importadas del horario. */
	private String conDosEnCurso(String correo) throws Exception {
		String token = entrar(correo);
		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones/horario").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"periodo":"2026-2","asignaturas":[
								  {"codigo":"CAD612021519","grupo":"SIS.FUSA 501M","clases":[]},
								  {"codigo":"CAD612021520","grupo":"SIS.FUSA 501M","clases":[]}]}"""))
				.andExpect(status().isCreated());
		return token;
	}

	private int matriculaDeRedes(String token) throws Exception {
		String lista = mvc.perform(get("/api/mis/matriculas?semestre=actual").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(lista, "$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(REDES));
		return ids.getFirst();
	}

	private void definirEstructura(String token, int matricula) throws Exception {
		mvc.perform(put("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(ESTRUCTURA))
				.andExpect(status().isOk());
	}

	private void anotar(String token, int matricula, int categoria, int actividad, String nota) throws Exception {
		mvc.perform(put("/api/mis/matriculas/%d/evaluacion/categorias/%d/actividades/%d/calificacion"
						.formatted(matricula, categoria, actividad))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nota\":" + nota + "}"))
				.andExpect(status().isOk());
	}

	private ResultActions brujula(String token) throws Exception {
		return mvc.perform(get("/api/mis/brujula").header("Authorization", "Bearer " + token));
	}

	private ResultActions cambiarUmbrales(String token, String cuerpo) throws Exception {
		return mvc.perform(put("/api/mis/configuracion/umbrales").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	@Test
	void sinEstructuraDeEvaluacionNadaTieneDatosYSeDiceQueHaceFalta() throws Exception {
		String token = conDosEnCurso("brujula.sindatos@ucundinamarca.edu.co");

		brujula(token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asignaturas.length()").value(2))
				.andExpect(jsonPath("$.asignaturas[*].nivel").value(Matchers.everyItem(Matchers.is("sin_datos"))))
				.andExpect(jsonPath("$.asignaturas[0].explicacion").value(Matchers.containsString("Cuéntanos cómo te evalúan")))
				.andExpect(jsonPath("$.asignaturas[0].notaRequerida").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.meta").value(3.0))
				.andExpect(jsonPath("$.umbrales.medio").value(3.5))
				.andExpect(jsonPath("$.umbrales.alto").value(4.5));
	}

	@Test
	void conEstructuraElNivelSaleDeLaNotaQueHaceFaltaYLoQueNoTieneDatosVaAlFinal() throws Exception {
		String token = conDosEnCurso("brujula.bajo@ucundinamarca.edu.co");
		definirEstructura(token, matriculaDeRedes(token));

		brujula(token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asignaturas[0].asignatura.codigoAsignatura").value(REDES))
				.andExpect(jsonPath("$.asignaturas[0].nivel").value("bajo"))
				.andExpect(jsonPath("$.asignaturas[0].motivo").value("dentro_de_lo_alcanzable"))
				.andExpect(jsonPath("$.asignaturas[0].notaRequerida").value(3.0))
				.andExpect(jsonPath("$.asignaturas[0].explicacion").value(Matchers.containsString("Necesitas 3.00")))
				.andExpect(jsonPath("$.asignaturas[1].nivel").value("sin_datos"));
	}

	@Test
	void cuandoLaMetaYaNoSeAlcanzaElRiesgoEsAltoYLoExplica() throws Exception {
		String token = conDosEnCurso("brujula.alto@ucundinamarca.edu.co");
		int matricula = matriculaDeRedes(token);
		definirEstructura(token, matricula);
		anotar(token, matricula, 1, 1, "0.0");
		anotar(token, matricula, 1, 2, "0.0");
		anotar(token, matricula, 2, 1, "0.0");
		mvc.perform(patch("/api/mis/configuracion").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"meta\":4.5}"))
				.andExpect(status().isOk());

		// Queda 40 % por evaluar: con 5.0 en todo se llega a 2.00 y la meta es 4.5.
		brujula(token)
				.andExpect(jsonPath("$.asignaturas[0].nivel").value("alto"))
				.andExpect(jsonPath("$.asignaturas[0].motivo").value("inalcanzable"))
				.andExpect(jsonPath("$.asignaturas[0].notaRequerida").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.asignaturas[0].maximoPosible").value(2.0))
				.andExpect(jsonPath("$.asignaturas[0].explicacion").value(Matchers.containsString("llegarías a 2.00")));
	}

	@Test
	void cambiarLosUmbralesRecalculaLaBrujulaEnLaSiguienteConsulta() throws Exception {
		String token = conDosEnCurso("brujula.umbrales@ucundinamarca.edu.co");
		int matricula = matriculaDeRedes(token);
		definirEstructura(token, matricula);
		anotar(token, matricula, 1, 1, "4.0");

		// Parcial 4.0: falta (3.0 - 0.84) / 0.79 = 2.74. Con los umbrales por defecto es riesgo bajo.
		brujula(token).andExpect(jsonPath("$.asignaturas[0].nivel").value("bajo"));

		cambiarUmbrales(token, "{\"medio\":2.0,\"alto\":2.5}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.umbrales.medio").value(2.0))
				.andExpect(jsonPath("$.umbrales.alto").value(2.5))
				.andExpect(jsonPath("$.meta").value(3.0));

		brujula(token)
				.andExpect(jsonPath("$.umbrales.alto").value(2.5))
				.andExpect(jsonPath("$.asignaturas[0].nivel").value("alto"))
				.andExpect(jsonPath("$.asignaturas[0].motivo").value("sobre_el_umbral_alto"))
				.andExpect(jsonPath("$.asignaturas[0].explicacion").value(Matchers.containsString("umbral de riesgo alto (2.5)")));

		mvc.perform(get("/api/mis/configuracion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.umbrales.medio").value(2.0));
	}

	@Test
	void unosUmbralesInvalidosSeRechazanSinGuardarlos() throws Exception {
		String token = entrar("brujula.invalidos@ucundinamarca.edu.co");

		cambiarUmbrales(token, "{\"medio\":4.5,\"alto\":3.5}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("menor que el alto")));
		cambiarUmbrales(token, "{\"medio\":3.0,\"alto\":5.5}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("0.0 y 5.0")));
		cambiarUmbrales(token, "{\"medio\":3.0}")
				.andExpect(status().isBadRequest());

		mvc.perform(get("/api/mis/configuracion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.umbrales.medio").value(3.5))
				.andExpect(jsonPath("$.umbrales.alto").value(4.5));
	}

	@Test
	void sinSesionNoSeAtiende() throws Exception {
		mvc.perform(get("/api/mis/brujula")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/mis/configuracion/umbrales").contentType(MediaType.APPLICATION_JSON)
						.content("{\"medio\":3.0,\"alto\":4.0}"))
				.andExpect(status().isUnauthorized());
	}
}
