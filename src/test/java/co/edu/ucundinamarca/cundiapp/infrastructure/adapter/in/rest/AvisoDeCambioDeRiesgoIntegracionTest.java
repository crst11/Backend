package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Avisar cuando cambia el riesgo de una asignatura (RF10, SCRUM-50), con seguridad real y PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: el aviso sale al moverse el nivel por una nota, por los umbrales o al quitar la
 * nota; no sale la primera vez ni cuando el nivel se queda igual; y lo que la persona apagó se respeta sin que
 * al volver a encender le lleguen cambios viejos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AvisoDeCambioDeRiesgoIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String REDES = "CAD612021519";
	private static final String UMBRALES_BAJOS = "{\"medio\":2.0,\"alto\":2.5}";
	private static final String UMBRALES_POR_DEFECTO = "{\"medio\":3.5,\"alto\":4.5}";

	private static final String ESTRUCTURA = """
			{"categorias":[
			  {"consecutivo":1,"nombre":"Primer corte","porcentaje":30,"origen":"estudiante","actividades":[
			    {"consecutivo":1,"nombre":"Parcial","porcentaje":70,"tipo":"parcial"},
			    {"consecutivo":2,"nombre":"Taller","porcentaje":30,"tipo":"taller"}]},
			  {"consecutivo":2,"nombre":"Segundo corte","porcentaje":30,"origen":"estudiante","actividades":[
			    {"consecutivo":1,"nombre":"Proyecto","porcentaje":100,"tipo":"taller"}]},
			  {"consecutivo":3,"nombre":"Tercer corte","porcentaje":40,"origen":"estudiante","actividades":[]}]}""";

	private record Sesion(String token, int id, int matricula) {
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

	/** Una persona con Redes en curso y su estructura de evaluación ya definida: el nivel de partida es «bajo». */
	private Sesion conRedesDefinida(String usuario) throws Exception {
		String correo = usuario + "@ucundinamarca.edu.co";
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true, Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		MvcResult login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones/horario").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"periodo":"2026-2","asignaturas":[
								  {"codigo":"CAD612021519","grupo":"SIS.FUSA 501M","clases":[]}]}"""))
				.andExpect(status().isCreated());
		String lista = mvc.perform(get("/api/mis/matriculas?semestre=actual").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(lista, "$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(REDES));
		int matricula = ids.getFirst();
		mvc.perform(put("/api/mis/matriculas/" + matricula + "/evaluacion").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(ESTRUCTURA))
				.andExpect(status().isOk());
		return new Sesion(token, creada.id(), matricula);
	}

	private void anotar(Sesion s, int categoria, int actividad, String nota) throws Exception {
		mvc.perform(put("/api/mis/matriculas/%d/evaluacion/categorias/%d/actividades/%d/calificacion"
						.formatted(s.matricula(), categoria, actividad))
						.header("Authorization", "Bearer " + s.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"nota\":" + nota + "}"))
				.andExpect(status().isOk());
	}

	private void umbrales(Sesion s, String cuerpo) throws Exception {
		mvc.perform(put("/api/mis/configuracion/umbrales").header("Authorization", "Bearer " + s.token())
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isOk());
	}

	private void preferencias(Sesion s, boolean activos, boolean riesgo) throws Exception {
		mvc.perform(put("/api/mis/configuracion/avisos").header("Authorization", "Bearer " + s.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"activos\":%s,\"vencimientos\":true,\"cambioDeRiesgo\":%s,\"anticipacionEnHoras\":24}"
								.formatted(activos, riesgo)))
				.andExpect(status().isOk());
	}

	private ResultActions avisos(Sesion s) throws Exception {
		return mvc.perform(get("/api/mis/avisos").header("Authorization", "Bearer " + s.token()));
	}

	private String nivelGuardado(Sesion s) {
		return jdbc.queryForObject("SELECT nivel FROM riesgo_avisado WHERE id_matricula = ?", String.class, s.matricula());
	}

	@Test
	void laPrimeraVezSoloSeGuardaElNivelYNoSeAvisa() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.primera.vez");

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
		assertThat(nivelGuardado(ana)).isEqualTo("bajo");
	}

	@Test
	void subirLosUmbralesHastaQueLaNotaQueHaceFaltaLosSuperaAvisaQueSubioElRiesgo() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.umbrales.suben");

		umbrales(ana, UMBRALES_BAJOS);

		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(1))
				.andExpect(jsonPath("$.avisos[0].tipo").value("cambio_riesgo"))
				.andExpect(jsonPath("$.avisos[0].titulo").value("Subió tu riesgo"))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.endsWith("pasó de bajo a alto.")))
				.andExpect(jsonPath("$.avisos[0].leido").value(false));
		assertThat(nivelGuardado(ana)).isEqualTo("alto");
	}

	@Test
	void devolverLosUmbralesAvisaQueBajoElRiesgo() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.umbrales.bajan");
		umbrales(ana, UMBRALES_BAJOS);

		umbrales(ana, UMBRALES_POR_DEFECTO);

		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(2))
				.andExpect(jsonPath("$.avisos[0].titulo").value("Bajó tu riesgo"))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.endsWith("pasó de alto a bajo.")));
	}

	@Test
	void quedarseEnElMismoNivelNoGeneraOtroAviso() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.mismo.nivel");

		umbrales(ana, "{\"medio\":3.2,\"alto\":4.6}");

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void lasNotasQueEmpujanLaMetaFueraDeAlcanceAvisanCadaSaltoDeNivel() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.notas");

		anotar(ana, 1, 1, "0.0");
		anotar(ana, 1, 2, "0.0");
		anotar(ana, 2, 1, "0.0");

		// 0.0 al parcial: bajo → medio; al proyecto, con 60 % ya perdido: medio → alto.
		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(2))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.endsWith("pasó de medio a alto.")))
				.andExpect(jsonPath("$.avisos[1].mensaje").value(Matchers.endsWith("pasó de bajo a medio.")));
	}

	@Test
	void quitarLaNotaQueLoSubioDevuelveElNivelYAvisa() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.quitar.nota");
		anotar(ana, 1, 1, "0.0");
		mvc.perform(delete("/api/mis/matriculas/%d/evaluacion/categorias/1/actividades/1/calificacion".formatted(ana.matricula()))
						.header("Authorization", "Bearer " + ana.token()))
				.andExpect(status().isOk());

		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(2))
				.andExpect(jsonPath("$.avisos[0].titulo").value("Bajó tu riesgo"))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.endsWith("pasó de medio a bajo.")));
		assertThat(nivelGuardado(ana)).isEqualTo("bajo");
	}

	@Test
	void conElAvisoDeRiesgoApagadoNoSaleNadaPeroAlEncenderNoLlegaLoViejo() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.apagado");
		preferencias(ana, true, false);

		umbrales(ana, UMBRALES_BAJOS);

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
		assertThat(nivelGuardado(ana)).isEqualTo("alto");

		preferencias(ana, true, true);
		umbrales(ana, "{\"medio\":2.0,\"alto\":2.6}");
		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));

		umbrales(ana, UMBRALES_POR_DEFECTO);
		avisos(ana)
				.andExpect(jsonPath("$.avisos.length()").value(1))
				.andExpect(jsonPath("$.avisos[0].mensaje").value(Matchers.endsWith("pasó de alto a bajo.")));
	}

	@Test
	void conElInterruptorGeneralApagadoTampocoSaleNada() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.general.apagado");
		preferencias(ana, false, true);

		umbrales(ana, UMBRALES_BAJOS);

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void elAvisoDeUnEstudianteNoApareceEnLaBandejaDeOtro() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.aislado.ana");
		Sesion beto = conRedesDefinida("riesgo.aislado.beto");

		umbrales(ana, UMBRALES_BAJOS);

		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(1));
		avisos(beto).andExpect(jsonPath("$.avisos.length()").value(0));
	}

	@Test
	void siSeBorraLaMatriculaSeVaConSuNivelYSusAvisos() throws Exception {
		Sesion ana = conRedesDefinida("riesgo.borrar.matricula");
		umbrales(ana, UMBRALES_BAJOS);

		jdbc.update("DELETE FROM matricula_asignatura WHERE id_matricula = ?", ana.matricula());

		assertThat(jdbc.queryForObject("SELECT count(*) FROM riesgo_avisado WHERE id_matricula = ?", Integer.class, ana.matricula()))
				.isZero();
		avisos(ana).andExpect(jsonPath("$.avisos.length()").value(0));
	}
}
