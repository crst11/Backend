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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Ver mi horario con ubicación (RF04, SCRUM-25), con seguridad real y PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: un bloque agregado a mano aparece en el horario con su salón, su
 * bloque, su sede, su hora y su docente; el horario sale ordenado por día y por hora; el salón y el
 * docente se reutilizan en vez de duplicarse; y lo que no tiene sentido (horas al revés, un día que
 * no existe, la asignatura de otro, una materia ya terminada) se rechaza con un mensaje útil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HorarioIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String ALGEBRA = "CAD612021101";
	private static final String ELECTRONICA = "CAD612021106";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

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
	 * Dos asignaturas que el estudiante está cursando. Hoy la única forma de tener matrículas es
	 * importar el registro extendido; sin nota, una asignatura queda "en curso".
	 */
	private String cursando(String correo) throws Exception {
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
								  {"codigo":"2026-2","creditosMatriculados":7,"creditosAprobados":0,
								   "notas":[{"codigoAsignatura":"%s","nota":null},
								             {"codigoAsignatura":"%s","nota":null}]}]}"""
								.formatted(ALGEBRA, ELECTRONICA)))
				.andExpect(status().isCreated());
		return token;
	}

	private int matriculaDe(String token, String codigoAsignatura) throws Exception {
		String lista = mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(lista, "$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(codigoAsignatura));
		return ids.getFirst();
	}

	private ResultActions agregar(String token, int matricula, String cuerpo) throws Exception {
		return mvc.perform(post("/api/mis/matriculas/" + matricula + "/horario")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	private static String clase(String dia, String inicio, String fin) {
		return """
				{"dia":"%s","horaInicio":"%s","horaFin":"%s","salon":"F-203","bloque":"Bloque F",
				 "sede":"Fusagasugá","docente":{"nombres":"María José","apellidos":"Pérez Gómez"}}"""
				.formatted(dia, inicio, fin);
	}

	@Test
	void unBloqueAgregadoApareceConSuUbicacionSuHoraYSuDocente() throws Exception {
		String token = cursando("horario.agregar@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);

		agregar(token, algebra, clase("lunes", "08:00", "10:00"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.consecutivo").value(1))
				.andExpect(jsonPath("$.asignatura").value(Matchers.not(Matchers.emptyString())))
				.andExpect(jsonPath("$.horaInicio").value("08:00"))
				.andExpect(jsonPath("$.horaFin").value("10:00"))
				.andExpect(jsonPath("$.duracionEnMinutos").value(120))
				.andExpect(jsonPath("$.salon").value("F-203"));

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalDeClases").value(1))
				.andExpect(jsonPath("$.dias.length()").value(1))
				.andExpect(jsonPath("$.dias[0].dia").value("lunes"))
				.andExpect(jsonPath("$.dias[0].clases[0].codigoAsignatura").value(ALGEBRA))
				.andExpect(jsonPath("$.dias[0].clases[0].salon").value("F-203"))
				.andExpect(jsonPath("$.dias[0].clases[0].bloque").value("Bloque F"))
				.andExpect(jsonPath("$.dias[0].clases[0].sede").value("Fusagasugá"))
				.andExpect(jsonPath("$.dias[0].clases[0].docente.nombres").value("María José"))
				.andExpect(jsonPath("$.dias[0].clases[0].docente.apellidos").value("Pérez Gómez"));
	}

	@Test
	void unHorarioSinClasesEsUnaListaVaciaYNoUnError() throws Exception {
		String token = cursando("horario.vacio@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalDeClases").value(0))
				.andExpect(jsonPath("$.dias.length()").value(0));
	}

	@Test
	void elHorarioSaleOrdenadoPorDiaYPorHoraSinImportarComoSeAgregoYDeDosAsignaturas() throws Exception {
		String token = cursando("horario.orden@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);
		int electronica = matriculaDe(token, ELECTRONICA);

		agregar(token, algebra, clase("viernes", "14:00", "16:00")).andExpect(status().isCreated());
		agregar(token, electronica, clase("lunes", "10:00", "12:00")).andExpect(status().isCreated());
		agregar(token, algebra, clase("lunes", "08:00", "10:00")).andExpect(status().isCreated());

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(3))
				.andExpect(jsonPath("$.dias[0].dia").value("lunes"))
				.andExpect(jsonPath("$.dias[0].clases[0].horaInicio").value("08:00"))
				.andExpect(jsonPath("$.dias[0].clases[1].horaInicio").value("10:00"))
				.andExpect(jsonPath("$.dias[1].dia").value("viernes"));
	}

	@Test
	void unSegundoBloqueDeLaMismaAsignaturaRecibeElSiguienteNumeroDeOrden() throws Exception {
		String token = cursando("horario.consecutivo@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);

		agregar(token, algebra, clase("lunes", "08:00", "10:00")).andExpect(jsonPath("$.consecutivo").value(1));
		agregar(token, algebra, clase("miercoles", "08:00", "10:00")).andExpect(jsonPath("$.consecutivo").value(2));
	}

	@Test
	void elSalonYElDocenteSeReutilizanEnVezDeDuplicarse() throws Exception {
		String token = cursando("horario.reutiliza@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);
		int electronica = matriculaDe(token, ELECTRONICA);

		agregar(token, algebra, clase("lunes", "08:00", "10:00")).andExpect(status().isCreated());
		// El mismo docente escrito distinto: otra mayúscula, sin tildes, con un espacio de más.
		agregar(token, electronica, """
				{"dia":"martes","horaInicio":"08:00","horaFin":"10:00","salon":"F-203","bloque":"Bloque F",
				 "sede":"Fusagasugá","docente":{"nombres":"MARIA  JOSE","apellidos":"perez gomez"}}""")
				.andExpect(status().isCreated());

		Integer docentes = jdbc.queryForObject(
				"SELECT count(*) FROM docente WHERE nombre_normalizado = 'maria jose perez gomez'", Integer.class);
		Integer espacios = jdbc.queryForObject(
				"SELECT count(*) FROM espacio_fisico WHERE nomenclatura = 'F-203' AND bloque = 'Bloque F' AND sede = 'Fusagasugá'",
				Integer.class);
		org.assertj.core.api.Assertions.assertThat(docentes).isEqualTo(1);
		org.assertj.core.api.Assertions.assertThat(espacios).isEqualTo(1);
	}

	@Test
	void unBloqueSinSalonNiDocenteSeAceptaYSeMuestraSinEllos() throws Exception {
		String token = cursando("horario.sinlugar@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);

		agregar(token, algebra, "{\"dia\":\"sabado\",\"horaInicio\":\"07:00\",\"horaFin\":\"09:30\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.salon").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.docente").value(Matchers.nullValue()));

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.dias[0].clases[0].salon").value(Matchers.nullValue()))
				.andExpect(jsonPath("$.dias[0].clases[0].duracionEnMinutos").value(150));
	}

	@Test
	void lasHorasAlReves_unDiaInventado_oUnSalonSinSedeSeRechazanConUnMensajeUtil() throws Exception {
		String token = cursando("horario.invalido@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);

		agregar(token, algebra, clase("lunes", "10:00", "08:00"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("empezar antes de terminar")));
		agregar(token, algebra, clase("lunas", "08:00", "10:00"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("lunas")));
		agregar(token, algebra, "{\"dia\":\"lunes\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\",\"salon\":\"F-203\"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("sede")));
		// Una hora que no es una hora no es una regla del negocio: es una petición mal armada.
		agregar(token, algebra, "{\"dia\":\"lunes\",\"horaInicio\":\"mañana\",\"horaFin\":\"10:00\"}")
				.andExpect(status().isBadRequest());
		agregar(token, algebra, "{\"dia\":\"lunes\"}").andExpect(status().isBadRequest());

		// Nada de lo anterior dejó un bloque a medias.
		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(0));
	}

	@Test
	void quitarUnBloqueLoSacaDelHorarioYQuitarloDeNuevoSeRechaza() throws Exception {
		String token = cursando("horario.quitar@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);
		agregar(token, algebra, clase("lunes", "08:00", "10:00")).andExpect(status().isCreated());

		mvc.perform(delete("/api/mis/matriculas/" + algebra + "/horario/1").header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(0));

		mvc.perform(delete("/api/mis/matriculas/" + algebra + "/horario/1").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos esa clase")));
	}

	@Test
	void laAsignaturaDeOtroEstudianteNoSeToca() throws Exception {
		String dueno = cursando("horario.dueno@ucundinamarca.edu.co");
		int matricula = matriculaDe(dueno, ALGEBRA);
		agregar(dueno, matricula, clase("lunes", "08:00", "10:00")).andExpect(status().isCreated());
		String otro = entrar("horario.intruso@ucundinamarca.edu.co");

		// "No la encontramos", no un 403: un 403 confirmaría que esa matrícula existe.
		agregar(otro, matricula, clase("martes", "08:00", "10:00"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("No encontramos esa asignatura")));
		mvc.perform(delete("/api/mis/matriculas/" + matricula + "/horario/1").header("Authorization", "Bearer " + otro))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + otro))
				.andExpect(jsonPath("$.totalDeClases").value(0));

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + dueno))
				.andExpect(jsonPath("$.totalDeClases").value(1));
	}

	@Test
	void unaAsignaturaYaTerminadaNoRecibeHorarioYSusBloquesNoSalenEnElDeHoy() throws Exception {
		String token = cursando("horario.terminada@ucundinamarca.edu.co");
		int algebra = matriculaDe(token, ALGEBRA);
		agregar(token, algebra, clase("lunes", "08:00", "10:00")).andExpect(status().isCreated());

		// La materia se da por aprobada: deja de ser parte del horario de este período.
		jdbc.update("UPDATE matricula_asignatura SET estado_matricula = 'aprobada', nota_definitiva = 4.0 "
				+ "WHERE id_matricula = ?", algebra);

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(0));
		agregar(token, algebra, clase("martes", "08:00", "10:00"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("estás cursando")));
	}

	@Test
	void sinSesionNoSeVeNiSeAgregaNada() throws Exception {
		mvc.perform(get("/api/mis/horario")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/mis/matriculas/1/horario").contentType(MediaType.APPLICATION_JSON)
				.content(clase("lunes", "08:00", "10:00"))).andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/mis/matriculas/1/horario/1")).andExpect(status().isUnauthorized());
	}
}
