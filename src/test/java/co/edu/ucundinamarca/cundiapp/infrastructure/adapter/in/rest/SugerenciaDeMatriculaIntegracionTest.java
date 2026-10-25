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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** La sugerencia de matrícula de extremo a extremo, con la seguridad real y PostgreSQL real (SCRUM-244). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SugerenciaDeMatriculaIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String RUTA = "/api/mis/horario/sugerencia-de-matricula";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

	private int idDe(String correo) {
		return jdbc.queryForObject(
				"SELECT id_estudiante FROM cundiapp.estudiante WHERE correo_institucional = ?", Integer.class, correo);
	}

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
		mvc.perform(put("/api/mis/perfil-academico")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
	}

	private void aprobarPrimerSemestre(int id) {
		jdbc.update("""
				INSERT INTO cundiapp.periodo_academico
				       (codigo_periodo, anio, semestre, fecha_inicio, fecha_fin, estado_periodo)
				VALUES ('2024-2', 2024, 2, make_date(2024, 1, 15), make_date(2024, 6, 15), 'finalizado')
				ON CONFLICT (codigo_periodo) DO NOTHING""");
		for (String codigo : new String[] {"CAD612021101", "CAD612021102", "CAD612021103", "CAD612021104",
				"CAD612021105", "CAD612021106", "DN-CAI1002020201", "DN-CAI1002020202"}) {
			jdbc.update("""
					INSERT INTO cundiapp.matricula_asignatura
					       (id_estudiante, codigo_asignatura, codigo_periodo, estado_matricula, nota_definitiva, fuente_notas)
					VALUES (?, ?, '2024-2', 'aprobada', CAST('4.0' AS NUMERIC), 'importacion')""", id, codigo);
		}
	}

	private ResultActions consultar(String token) throws Exception {
		return mvc.perform(get(RUTA).header("Authorization", "Bearer " + token));
	}

	@Test
	void sinSesionResponde401() throws Exception {
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
	}

	@Test
	void sinProgramaElegidoNoHaySugerenciaYDiceElMotivo() throws Exception {
		String token = entrar("sin.programa@ucundinamarca.edu.co");

		consultar(token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.semestreSugerido").doesNotExist())
				.andExpect(jsonPath("$.materias").isEmpty())
				.andExpect(jsonPath("$.motivo").value("Elige tu programa para recibir una sugerencia de matrícula."));
	}

	@Test
	void conProgramaPeroSinHistorialPideImportarElRegistro() throws Exception {
		String token = entrar("sin.historial@ucundinamarca.edu.co");
		elegirPrograma(token);

		consultar(token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.semestreSugerido").doesNotExist())
				.andExpect(jsonPath("$.motivo").value(
						"Importa tu Registro Académico Extendido para recibir una sugerencia de matrícula."));
	}

	@Test
	void conElPrimerSemestreAprobadoSugiereLasMateriasDelSegundo() throws Exception {
		String correo = "con.historial@ucundinamarca.edu.co";
		String token = entrar(correo);
		elegirPrograma(token);
		aprobarPrimerSemestre(idDe(correo));

		consultar(token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.semestreSugerido").value(2))
				.andExpect(jsonPath("$.motivo").doesNotExist())
				.andExpect(jsonPath("$.materias").isNotEmpty())
				.andExpect(jsonPath("$.materias[0].semestre").value(2))
				.andExpect(jsonPath("$.materias[0].motivoDeSugerencia").value("siguiente_semestre"))
				.andExpect(jsonPath("$.creditos").isNumber());
	}

	@Test
	void cadaEstudianteVeSoloLoSuyo() throws Exception {
		String conHistorial = "uno@ucundinamarca.edu.co";
		String tokenUno = entrar(conHistorial);
		elegirPrograma(tokenUno);
		aprobarPrimerSemestre(idDe(conHistorial));
		String tokenDos = entrar("dos@ucundinamarca.edu.co");
		elegirPrograma(tokenDos);

		consultar(tokenDos)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.semestreSugerido").doesNotExist())
				.andExpect(jsonPath("$.materias").isEmpty());
	}
}
