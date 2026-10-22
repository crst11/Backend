package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
 * Cambiar un grupo del horario armado (RF13, SCRUM-245), con seguridad real y PostgreSQL real. Lo que hay que
 * demostrar: exige sesión, dice por cada grupo candidato si cabe o con qué choca (materia, grupo, día y hora),
 * rechaza lo inválido con el código que toca, mira los tamaños antes de armar objetos y no guarda nada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VerificacionesDeHorarioIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String RUTA = "/api/mis/horario/verificaciones";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

	private String entrar(String usuario) throws Exception {
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
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	private ResultActions verificar(String token, String cuerpo) throws Exception {
		return mvc.perform(post(RUTA).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private static String bloque(String dia, String inicio, String fin) {
		return "{\"dia\":\"%s\",\"inicio\":\"%s\",\"fin\":\"%s\"}".formatted(dia, inicio, fin);
	}

	private static String grupo(String codigo, String... bloques) {
		return "{\"codigo\":\"%s\",\"bloques\":[%s]}".formatted(codigo, String.join(",", bloques));
	}

	private static String eleccion(String asignatura, String grupo) {
		return "{\"asignatura\":\"%s\",\"grupo\":%s}".formatted(asignatura, grupo);
	}

	private static String cuerpo(String asignaturaACambiar, String[] horario, String... candidatos) {
		return "{\"horario\":[%s],\"asignaturaACambiar\":\"%s\",\"candidatos\":[%s]}"
				.formatted(String.join(",", horario), asignaturaACambiar, String.join(",", candidatos));
	}

	private static final String[] HORARIO = {
		eleccion("Álgebra lineal", grupo("502", bloque("lunes", "07:00", "09:00"))),
		eleccion("Cálculo", grupo("501", bloque("martes", "09:00", "11:00"))),
	};

	// --- Lo que funciona -------------------------------------------------------------------------------------

	@Test
	void diceQueGrupoCabeYConQueMateriaGrupoDiaYHoraChocaElQueNo() throws Exception {
		String token = entrar("verifica.choque");

		verificar(token, cuerpo("Física", HORARIO,
				grupo("501", bloque("miercoles", "07:00", "09:00")),
				grupo("503", bloque("lunes", "08:00", "10:00"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.candidatos.length()").value(2))
				.andExpect(jsonPath("$.candidatos[0].grupo").value("501"))
				.andExpect(jsonPath("$.candidatos[0].cabe").value(true))
				.andExpect(jsonPath("$.candidatos[0].choques.length()").value(0))
				.andExpect(jsonPath("$.candidatos[1].grupo").value("503"))
				.andExpect(jsonPath("$.candidatos[1].cabe").value(false))
				.andExpect(jsonPath("$.candidatos[1].choques[0].asignatura").value("Álgebra lineal"))
				.andExpect(jsonPath("$.candidatos[1].choques[0].grupo").value("502"))
				.andExpect(jsonPath("$.candidatos[1].choques[0].dia").value("lunes"))
				.andExpect(jsonPath("$.candidatos[1].choques[0].inicio").value("07:00"))
				.andExpect(jsonPath("$.candidatos[1].choques[0].fin").value("09:00"));
	}

	@Test
	void unGrupoConElMismoHorarioQueElActualDeLaMateriaQueSeCambiaCabe() throws Exception {
		String token = entrar("verifica.mismo.horario");

		// 501 y 503 del caso del equipo: mismo horario, solo cambia el docente.
		verificar(token, cuerpo("Álgebra lineal", HORARIO, grupo("501", bloque("lunes", "07:00", "09:00"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.candidatos[0].cabe").value(true));
	}

	@Test
	void noGuardaNadaEnLaBaseDeDatos() throws Exception {
		String token = entrar("verifica.sin.guardar");
		Integer antes = jdbc.queryForObject("SELECT count(*) FROM cundiapp.bloque_de_horario", Integer.class);

		verificar(token, cuerpo("Física", HORARIO, grupo("501", bloque("miercoles", "07:00", "09:00"))))
				.andExpect(status().isOk());

		assertThat(jdbc.queryForObject("SELECT count(*) FROM cundiapp.bloque_de_horario", Integer.class)).isEqualTo(antes);
	}

	// --- Lo que se rechaza -----------------------------------------------------------------------------------

	@Test
	void sinSesionResponde401() throws Exception {
		mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo("Física", HORARIO, grupo("501", bloque("lunes", "07:00", "09:00")))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinLaMateriaACambiarOSinLosCandidatosResponde400() throws Exception {
		String token = entrar("verifica.incompleto");

		verificar(token, "{\"horario\":[],\"candidatos\":[%s]}".formatted(grupo("1", bloque("lunes", "07:00", "09:00"))))
				.andExpect(status().isBadRequest());
		verificar(token, "{\"horario\":[],\"asignaturaACambiar\":\"Física\"}").andExpect(status().isBadRequest());
		verificar(token, "{\"asignaturaACambiar\":\"Física\",\"candidatos\":[%s]}".formatted(grupo("1", bloque("lunes", "07:00", "09:00"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unaHoraQueNoEsUnaHoraResponde400() throws Exception {
		String token = entrar("verifica.hora.mala");

		verificar(token, cuerpo("Física", HORARIO, grupo("1", bloque("lunes", "mañana", "09:00"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unaReglaDeNegocioRotaResponde422ConElMotivo() throws Exception {
		String token = entrar("verifica.regla.rota");

		verificar(token, cuerpo("Física", HORARIO, grupo("1", bloque("lunes", "10:00", "08:00"))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("empezar antes de terminar")));
		verificar(token, cuerpo("Física", HORARIO, grupo("1", bloque("domingox", "08:00", "10:00"))))
				.andExpect(status().isUnprocessableEntity());
		verificar(token, "{\"horario\":[],\"asignaturaACambiar\":\"Física\",\"candidatos\":[]}")
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void pasarseDeLosLimitesResponde422SinArmarMillonesDeObjetos() throws Exception {
		String token = entrar("verifica.limites");
		String[] once = IntStream.range(0, 11)
				.mapToObj(i -> eleccion("M" + i, grupo("1", bloque("lunes", "07:00", "08:00"))))
				.toArray(String[]::new);
		String[] veintiuno = IntStream.range(0, 21)
				.mapToObj(i -> grupo("G" + i, bloque("lunes", "07:00", "08:00")))
				.toArray(String[]::new);
		String nueveBloques = IntStream.range(0, 9)
				.mapToObj(i -> bloque("lunes", "%02d:00".formatted(6 + i), "%02d:30".formatted(6 + i)))
				.collect(Collectors.joining(","));

		verificar(token, cuerpo("Física", once, grupo("1", bloque("lunes", "07:00", "08:00"))))
				.andExpect(status().isUnprocessableEntity());
		verificar(token, cuerpo("Física", HORARIO, veintiuno)).andExpect(status().isUnprocessableEntity());
		verificar(token, cuerpo("Física", HORARIO, "{\"codigo\":\"1\",\"bloques\":[" + nueveBloques + "]}"))
				.andExpect(status().isUnprocessableEntity());
	}
}
