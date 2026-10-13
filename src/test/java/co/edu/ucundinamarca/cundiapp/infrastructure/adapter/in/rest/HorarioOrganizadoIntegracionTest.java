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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Organizar mi horario a mi gusto (RF04, SCRUM-26), con seguridad real y PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: filtrar por asignatura o sede deja solo esas clases pero sigue
 * ofreciendo todas las opciones; ordenar cambia el orden dentro del día; la preferencia (vista, sede
 * y orden) se guarda, se lee de nuevo y es de cada estudiante; y lo que no vale se rechaza con un
 * mensaje útil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HorarioOrganizadoIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA_PROGRAMA = "109964";
	private static final String ALGEBRA = "CAD612021101";
	private static final String ELECTRONICA = "CAD612021106";

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

	private String cursando(String correo) throws Exception {
		String token = entrar(correo);
		mvc.perform(put("/api/mis/perfil-academico")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA_PROGRAMA)))
				.andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombreArchivo":"registro_extendido.pdf","detectadas":2,"periodos":[
								  {"codigo":"2026-2","creditosMatriculados":7,"creditosAprobados":0,
								   "notas":[{"codigoAsignatura":"%s","nota":null},
								             {"codigoAsignatura":"%s","nota":null}]}]}"""
								.formatted(ALGEBRA, ELECTRONICA)))
				.andExpect(status().isCreated());
		return token;
	}

	private int matriculaDe(String token, String codigo) throws Exception {
		String lista = mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(lista, "$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(codigo));
		return ids.getFirst();
	}

	private void clase(String token, int matricula, String dia, String inicio, String fin, String sede) throws Exception {
		mvc.perform(post("/api/mis/matriculas/" + matricula + "/horario")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"dia":"%s","horaInicio":"%s","horaFin":"%s","salon":"F-1","sede":"%s"}"""
								.formatted(dia, inicio, fin, sede)))
				.andExpect(status().isCreated());
	}

	/**
	 * Álgebra en Fusagasugá (lunes 10 a 12 y miércoles 8 a 10) y Electrónica en Soacha (lunes 8 a 10):
	 * el lunes por hora es Electrónica y después Álgebra; por asignatura, al revés.
	 */
	private String conHorario(String correo) throws Exception {
		String token = cursando(correo);
		int algebra = matriculaDe(token, ALGEBRA);
		int electronica = matriculaDe(token, ELECTRONICA);
		clase(token, algebra, "lunes", "10:00", "12:00", "Fusagasugá");
		clase(token, algebra, "miercoles", "08:00", "10:00", "Fusagasugá");
		clase(token, electronica, "lunes", "08:00", "10:00", "Soacha");
		return token;
	}

	private ResultActions horario(String token, String consulta) throws Exception {
		return mvc.perform(get("/api/mis/horario" + consulta).header("Authorization", "Bearer " + token));
	}

	private ResultActions guardarPreferencia(String token, String cuerpo) throws Exception {
		return mvc.perform(put("/api/mis/preferencias-de-horario")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	@Test
	void sinFiltroTraeTodoYOfreceLasOpcionesParaElegir() throws Exception {
		String token = conHorario("organizar.todo@ucundinamarca.edu.co");

		horario(token, "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalDeClases").value(3))
				.andExpect(jsonPath("$.sedes.length()").value(2))
				.andExpect(jsonPath("$.sedes[0]").value("Fusagasugá"))
				.andExpect(jsonPath("$.sedes[1]").value("Soacha"))
				.andExpect(jsonPath("$.asignaturas.length()").value(2))
				.andExpect(jsonPath("$.aplicado.orden").value("hora"))
				.andExpect(jsonPath("$.aplicado.sede").value(Matchers.nullValue()));
	}

	@Test
	void filtrarPorSedeDejaSoloLasClasesDeEsaSedeYSigueOfreciendoTodasLasOpciones() throws Exception {
		String token = conHorario("organizar.sede@ucundinamarca.edu.co");

		horario(token, "?sede=Soacha")
				.andExpect(jsonPath("$.totalDeClases").value(1))
				.andExpect(jsonPath("$.dias.length()").value(1))
				.andExpect(jsonPath("$.dias[0].clases[0].sede").value("Soacha"))
				// Sin esto, quien filtra por una sede no tendría cómo volver a elegir la otra.
				.andExpect(jsonPath("$.sedes.length()").value(2))
				.andExpect(jsonPath("$.aplicado.sede").value("Soacha"));
	}

	@Test
	void laSedeSeFiltraSinImportarMayusculasNiTildes() throws Exception {
		String token = conHorario("organizar.tildes@ucundinamarca.edu.co");

		horario(token, "?sede=FUSAGASUGA").andExpect(jsonPath("$.totalDeClases").value(2));
	}

	@Test
	void filtrarPorAsignaturaDejaSoloLasClasesDeEsaAsignatura() throws Exception {
		String token = conHorario("organizar.asignatura@ucundinamarca.edu.co");

		horario(token, "?asignatura=" + ALGEBRA)
				.andExpect(jsonPath("$.totalDeClases").value(2))
				.andExpect(jsonPath("$.dias[0].clases[0].codigoAsignatura").value(ALGEBRA))
				.andExpect(jsonPath("$.asignaturas.length()").value(2));
	}

	@Test
	void asignaturaYSedeJuntasSeExigenAmbas() throws Exception {
		String token = conHorario("organizar.ambas@ucundinamarca.edu.co");

		horario(token, "?asignatura=" + ALGEBRA + "&sede=Fusagasugá").andExpect(jsonPath("$.totalDeClases").value(2));
		horario(token, "?asignatura=" + ELECTRONICA + "&sede=Fusagasugá").andExpect(jsonPath("$.totalDeClases").value(0));
	}

	@Test
	void unFiltroSinCoincidenciasEsUnHorarioVacioConLasOpcionesIntactas() throws Exception {
		String token = conHorario("organizar.vacio@ucundinamarca.edu.co");

		horario(token, "?sede=Bogotá")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalDeClases").value(0))
				.andExpect(jsonPath("$.dias.length()").value(0))
				.andExpect(jsonPath("$.sedes.length()").value(2));
	}

	@Test
	void ordenarPorAsignaturaCambiaElOrdenDentroDelDia() throws Exception {
		String token = conHorario("organizar.orden@ucundinamarca.edu.co");

		// Por hora, el lunes arranca con Electrónica (8:00) y sigue Álgebra (10:00).
		horario(token, "")
				.andExpect(jsonPath("$.dias[0].dia").value("lunes"))
				.andExpect(jsonPath("$.dias[0].clases[0].horaInicio").value("08:00"));
		// Por asignatura, Álgebra va antes que Electrónica aunque sea más tarde.
		horario(token, "?orden=asignatura")
				.andExpect(jsonPath("$.aplicado.orden").value("asignatura"))
				.andExpect(jsonPath("$.dias[0].clases[0].codigoAsignatura").value(ALGEBRA))
				.andExpect(jsonPath("$.dias[0].clases[0].horaInicio").value("10:00"));
	}

	@Test
	void unOrdenQueNoExisteSeRechazaDiciendoCualesHay() throws Exception {
		String token = conHorario("organizar.ordeninvalido@ucundinamarca.edu.co");

		horario(token, "?orden=color")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("hora o asignatura")));
	}

	@Test
	void quienNuncaEligioNadaTienePreferenciaPredeterminada() throws Exception {
		String token = entrar("organizar.defecto@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/preferencias-de-horario").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vista").value("dia"))
				.andExpect(jsonPath("$.sede").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.orden").value("hora"));
	}

	@Test
	void laPreferenciaSeGuardaYSeVuelveALeer() throws Exception {
		String token = entrar("organizar.guardar@ucundinamarca.edu.co");

		guardarPreferencia(token, "{\"vista\":\"semana\",\"sede\":\" Fusagasugá \",\"orden\":\"asignatura\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vista").value("semana"))
				.andExpect(jsonPath("$.sede").value("Fusagasugá"))
				.andExpect(jsonPath("$.orden").value("asignatura"));

		mvc.perform(get("/api/mis/preferencias-de-horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.vista").value("semana"))
				.andExpect(jsonPath("$.sede").value("Fusagasugá"))
				.andExpect(jsonPath("$.orden").value("asignatura"));

		// Se puede volver a «todas las sedes»: la sede nula borra la elegida.
		guardarPreferencia(token, "{\"vista\":\"dia\",\"orden\":\"hora\"}")
				.andExpect(jsonPath("$.sede").value(Matchers.nullValue()));
	}

	@Test
	void laPreferenciaEsDeCadaEstudiante() throws Exception {
		String una = entrar("organizar.una@ucundinamarca.edu.co");
		String otra = entrar("organizar.otra@ucundinamarca.edu.co");

		guardarPreferencia(una, "{\"vista\":\"semana\",\"sede\":\"Soacha\",\"orden\":\"asignatura\"}").andExpect(status().isOk());

		mvc.perform(get("/api/mis/preferencias-de-horario").header("Authorization", "Bearer " + otra))
				.andExpect(jsonPath("$.vista").value("dia"))
				.andExpect(jsonPath("$.sede").value(Matchers.nullValue()));
	}

	@Test
	void unaPreferenciaInvalidaSeRechazaYNoDejaNadaAMedias() throws Exception {
		String token = entrar("organizar.invalida@ucundinamarca.edu.co");
		guardarPreferencia(token, "{\"vista\":\"semana\",\"orden\":\"hora\"}").andExpect(status().isOk());

		guardarPreferencia(token, "{\"vista\":\"mes\",\"orden\":\"hora\"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("dia o semana")));
		guardarPreferencia(token, "{\"vista\":\"dia\",\"orden\":\"color\"}").andExpect(status().isUnprocessableEntity());
		guardarPreferencia(token, "{\"vista\":\"dia\",\"orden\":\"hora\",\"sede\":\"" + "x".repeat(61) + "\"}")
				.andExpect(status().isUnprocessableEntity());
		// Sin vista ni orden es una petición mal armada, no una regla del negocio.
		guardarPreferencia(token, "{}").andExpect(status().isBadRequest());

		mvc.perform(get("/api/mis/preferencias-de-horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.vista").value("semana"));
	}

	@Test
	void sinSesionNoSeLeeNiSeGuardaLaPreferencia() throws Exception {
		mvc.perform(get("/api/mis/preferencias-de-horario")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/mis/preferencias-de-horario").contentType(MediaType.APPLICATION_JSON)
				.content("{\"vista\":\"dia\",\"orden\":\"hora\"}")).andExpect(status().isUnauthorized());
	}
}
