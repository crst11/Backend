package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Registrar mis calificaciones (RF05, SCRUM-28), con seguridad real y PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: la nota queda guardada y la actividad pasa a calificada (la base de
 * datos no deja calificar una actividad sin entregar), el acumulado sale del dominio con el peso
 * real de cada actividad, quitar una nota la devuelve a no entregada, y una nota mal escrita o de
 * una asignatura ajena se rechaza con un mensaje útil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CalificacionesIntegracionTest {

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

	private static String ruta(int matricula, int categoria, int actividad) {
		return "/api/mis/matriculas/%d/evaluacion/categorias/%d/actividades/%d/calificacion"
				.formatted(matricula, categoria, actividad);
	}

	private org.springframework.test.web.servlet.ResultActions poner(
			String token, int matricula, int categoria, int actividad, String cuerpo) throws Exception {
		return mvc.perform(put(ruta(matricula, categoria, actividad))
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	@Test
	void anotarUnaNotaLaGuardaYDevuelveLoQueLlevoAcumulado() throws Exception {
		String token = conMatricula("notas.anotar@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		// Parcial 4.0: pesa 30 % x 70 % = 21 % de la materia y aporta 0.84.
		poner(token, matricula, 1, 1, "{\"nota\":4.0}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nota").value(4.0))
				.andExpect(jsonPath("$.acumulado.puntos").value(0.84))
				.andExpect(jsonPath("$.acumulado.porcentajeEvaluado").value(21.00))
				.andExpect(jsonPath("$.acumulado.promedioDeLoEvaluado").value(4.00))
				.andExpect(jsonPath("$.acumulado.actividadesCalificadas").value(1))
				.andExpect(jsonPath("$.acumulado.actividadesTotales").value(3));

		// La estructura ahora trae la nota, el estado de entrega y el acumulado.
		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.categorias[0].actividades[0].nota").value(4.0))
				.andExpect(jsonPath("$.categorias[0].actividades[0].estadoEntrega").value("calificada"))
				.andExpect(jsonPath("$.categorias[0].actividades[1].nota").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.categorias[0].actividades[1].estadoEntrega").value("no_entregada"))
				.andExpect(jsonPath("$.acumulado.puntos").value(0.84));
	}

	@Test
	void anotarDeNuevoCorrigeLaNotaEnVezDeDuplicarla() throws Exception {
		String token = conMatricula("notas.corregir@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		poner(token, matricula, 1, 1, "{\"nota\":2.0}").andExpect(status().isOk());
		poner(token, matricula, 1, 1, "{\"nota\":4.5}")
				.andExpect(status().isOk())
				// 4.5 x 0.21 = 0.945, que se muestra 0.95.
				.andExpect(jsonPath("$.acumulado.puntos").value(0.95))
				.andExpect(jsonPath("$.acumulado.actividadesCalificadas").value(1));
	}

	@Test
	void sumaVariasNotasConElPesoRealDeCadaActividad() throws Exception {
		String token = conMatricula("notas.varias@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		poner(token, matricula, 1, 1, "{\"nota\":4.0}").andExpect(status().isOk());
		// Proyecto 3.0: 30 % x 100 % = 30 % de la materia. 0.84 + 0.90 = 1.74 sobre 51 % evaluado = 3.41.
		poner(token, matricula, 2, 1, "{\"nota\":3.0}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.acumulado.puntos").value(1.74))
				.andExpect(jsonPath("$.acumulado.porcentajeEvaluado").value(51.00))
				.andExpect(jsonPath("$.acumulado.promedioDeLoEvaluado").value(3.41));
	}

	@Test
	void quitarUnaNotaDevuelveLaActividadANoEntregadaYRecalcula() throws Exception {
		String token = conMatricula("notas.quitar@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);
		poner(token, matricula, 1, 1, "{\"nota\":4.0}").andExpect(status().isOk());

		mvc.perform(delete(ruta(matricula, 1, 1)).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.puntos").value(0.00))
				.andExpect(jsonPath("$.actividadesCalificadas").value(0))
				.andExpect(jsonPath("$.promedioDeLoEvaluado").value(Matchers.nullValue()));

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.categorias[0].actividades[0].nota").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.categorias[0].actividades[0].estadoEntrega").value("no_entregada"));

		// Quitar otra vez lo que ya no está: la pantalla quedó desactualizada y se le dice.
		mvc.perform(delete(ruta(matricula, 1, 1)).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("no tiene una nota")));
	}

	@Test
	void unaNotaFueraDeLaEscalaOConMasDecimalesSeRechazaConUnMensajeUtil() throws Exception {
		String token = conMatricula("notas.escala@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);

		poner(token, matricula, 1, 1, "{\"nota\":5.5}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("0.0 y 5.0")));
		poner(token, matricula, 1, 1, "{\"nota\":-1}")
				.andExpect(status().isUnprocessableEntity());
		poner(token, matricula, 1, 1, "{\"nota\":4.255}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("dos decimales")));
		// Sin nota en el cuerpo no es una regla del negocio: es una petición mal armada.
		poner(token, matricula, 1, 1, "{}").andExpect(status().isBadRequest());

		// Nada de lo anterior dejó una nota a medias.
		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.acumulado.actividadesCalificadas").value(0));
	}

	@Test
	void sinEstructuraGuardadaOConUnaActividadQueNoExisteSeRechaza() throws Exception {
		String token = conMatricula("notas.sinestructura@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);

		poner(token, matricula, 1, 1, "{\"nota\":4.0}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("define cómo te evalúan")));

		definirEstructura(token, matricula);
		poner(token, matricula, 9, 9, "{\"nota\":4.0}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("no existe")));
		// El tercer corte existe pero no tiene actividades que calificar.
		poner(token, matricula, 3, 1, "{\"nota\":4.0}").andExpect(status().isUnprocessableEntity());
	}

	@Test
	void unaMatriculaAjenaNoSeToca() throws Exception {
		String dueno = conMatricula("notas.dueno@ucundinamarca.edu.co");
		int matricula = matriculaDe(dueno);
		definirEstructura(dueno, matricula);
		String otro = entrar("notas.intruso@ucundinamarca.edu.co");

		// "No la encontramos", no un 403: un 403 confirmaría que esa matrícula existe.
		poner(otro, matricula, 1, 1, "{\"nota\":5.0}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos esa asignatura")));
		mvc.perform(delete(ruta(matricula, 1, 1)).header("Authorization", "Bearer " + otro))
				.andExpect(status().isUnprocessableEntity());

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + dueno))
				.andExpect(jsonPath("$.acumulado.actividadesCalificadas").value(0));
	}

	@Test
	void sinSesionNoSeAnotaNada() throws Exception {
		mvc.perform(put(ruta(1, 1, 1)).contentType(MediaType.APPLICATION_JSON).content("{\"nota\":4.0}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete(ruta(1, 1, 1))).andExpect(status().isUnauthorized());
	}

	@Test
	void reconfigurarLaEstructuraConservaLasNotasDeLasActividadesQueSiguen() throws Exception {
		String token = conMatricula("notas.reconfigurar@ucundinamarca.edu.co");
		int matricula = matriculaDe(token);
		definirEstructura(token, matricula);
		poner(token, matricula, 1, 1, "{\"nota\":4.0}").andExpect(status().isOk());

		// Se guarda de nuevo la misma estructura (SCRUM-27): la actividad conserva su número de orden.
		definirEstructura(token, matricula);

		mvc.perform(get("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.categorias[0].actividades[0].nota").value(4.0))
				.andExpect(jsonPath("$.categorias[0].actividades[0].estadoEntrega").value("calificada"));
	}
}
