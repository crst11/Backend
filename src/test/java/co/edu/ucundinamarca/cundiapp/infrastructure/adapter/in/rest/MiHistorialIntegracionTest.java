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

/**
 * Mi historial de extremo a extremo con la seguridad real y PostgreSQL real (SCRUM-22).
 *
 * <p>Los datos cursados se siembran aquí porque la importación del PDF llega en SCRUM-23: esta
 * historia solo muestra lo que ya esté cargado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MiHistorialIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";

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

	private void crearPeriodo(String codigo, int anio, int semestre) {
		jdbc.update("""
				INSERT INTO cundiapp.periodo_academico
				       (codigo_periodo, anio, semestre, fecha_inicio, fecha_fin, estado_periodo)
				VALUES (?, ?, ?, make_date(?, 1, 15), make_date(?, 6, 15), 'finalizado')
				ON CONFLICT (codigo_periodo) DO NOTHING""", codigo, anio, semestre, anio, anio);
	}

	private void matricular(int id, String periodo, String asignatura, String nota, String estado) {
		jdbc.update("""
				INSERT INTO cundiapp.matricula_asignatura
				       (id_estudiante, codigo_asignatura, codigo_periodo, estado_matricula, nota_definitiva, fuente_notas)
				VALUES (?, ?, ?, ?, CAST(? AS NUMERIC), 'importacion')""",
				id, asignatura, periodo, estado, nota);
	}

	/** El primer período del plan de Fusagasugá, con las notas del registro extendido real. */
	private void sembrarPrimerPeriodo(int id) {
		crearPeriodo("2024-2", 2024, 2);
		matricular(id, "2024-2", "CAD612021101", "4.2", "aprobada");
		matricular(id, "2024-2", "CAD612021106", "4.3", "aprobada");
		matricular(id, "2024-2", "CAD612021103", "4.3", "aprobada");
		matricular(id, "2024-2", "CAD612021105", "4.6", "aprobada");
		matricular(id, "2024-2", "CAD612021102", "4.7", "aprobada");
		matricular(id, "2024-2", "CAD612021104", "4.8", "aprobada");
		// Las de diagnóstico y nivelatorio: valen 0 créditos y no ponderan.
		matricular(id, "2024-2", "DN-CAI1002020202", "4.0", "aprobada");
		matricular(id, "2024-2", "DN-CAI1002020201", "5.0", "aprobada");
	}

	@Test
	void muestraLasNotasAgrupadasPorPeriodo() throws Exception {
		String token = entrar("historial.notas@ucundinamarca.edu.co");
		elegirPrograma(token);
		sembrarPrimerPeriodo(idDe("historial.notas@ucundinamarca.edu.co"));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos.length()").value(1))
				.andExpect(jsonPath("$.periodos[0].codigo").value("2024-2"))
				.andExpect(jsonPath("$.periodos[0].asignaturas.length()").value(8))
				// Ponderación matriculada del reporte oficial: 16 créditos.
				.andExpect(jsonPath("$.periodos[0].creditosMatriculados").value(16))
				.andExpect(jsonPath("$.periodos[0].creditosAprobados").value(16));
	}

	@Test
	void sinReporteOficialMuestraElPromedioQueCalculaLaApp() throws Exception {
		String token = entrar("historial.calculado@ucundinamarca.edu.co");
		elegirPrograma(token);
		sembrarPrimerPeriodo(idDe("historial.calculado@ucundinamarca.edu.co"));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos[0].fuenteDelPromedio").value("calculado"))
				.andExpect(jsonPath("$.fuenteDelAcumulado").value("calculado"))
				// 71.3 puntos sobre 16 créditos: la universidad lo imprime como 4.4.
				.andExpect(jsonPath("$.periodos[0].promedio").value(4.46));
	}

	@Test
	void cuandoHayReporteOficialManda_aunqueNuestroCalculoDifiera() throws Exception {
		String correo = "historial.oficial@ucundinamarca.edu.co";
		String token = entrar(correo);
		elegirPrograma(token);
		int id = idDe(correo);
		sembrarPrimerPeriodo(id);
		// Lo que publica el registro extendido para ese período: 4.4, no 4.46.
		jdbc.update("""
				INSERT INTO cundiapp.resumen_periodo
				       (id_estudiante, codigo_periodo, creditos_matriculados, creditos_aprobados,
				        promedio_periodo, promedio_acumulado, fuente)
				VALUES (?, '2024-2', 16, 16, 4.4, 4.4, 'importacion')""", id);

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos[0].promedio").value(4.4))
				.andExpect(jsonPath("$.periodos[0].fuenteDelPromedio").value("oficial"))
				.andExpect(jsonPath("$.promedioAcumulado").value(4.4))
				.andExpect(jsonPath("$.fuenteDelAcumulado").value("oficial"));
	}

	@Test
	void siElReporteOficialQuedoViejoElAcumuladoVuelveAlCalculado() throws Exception {
		// El reporte cubre hasta 2024-2, pero el estudiante ya cursó 2025-1: seguir mostrando el
		// acumulado oficial le diría que su promedio es el de hace un semestre.
		String correo = "historial.desactualizado@ucundinamarca.edu.co";
		String token = entrar(correo);
		elegirPrograma(token);
		int id = idDe(correo);
		sembrarPrimerPeriodo(id);
		jdbc.update("""
				INSERT INTO cundiapp.resumen_periodo
				       (id_estudiante, codigo_periodo, creditos_matriculados, creditos_aprobados,
				        promedio_periodo, promedio_acumulado, fuente)
				VALUES (?, '2024-2', 16, 16, 4.4, 4.4, 'importacion')""", id);
		crearPeriodo("2025-1", 2025, 1);
		matricular(id, "2025-1", "CAD612021207", "5.0", "aprobada");

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				// El período viejo conserva su promedio oficial...
				.andExpect(jsonPath("$.periodos[0].fuenteDelPromedio").value("oficial"))
				.andExpect(jsonPath("$.periodos[0].promedio").value(4.4))
				// ...pero el acumulado ya no puede salir de ese reporte.
				.andExpect(jsonPath("$.fuenteDelAcumulado").value("calculado"))
				// 71.3 puntos de 2024-2 más 20 de Cálculo Diferencial, sobre 20 créditos.
				.andExpect(jsonPath("$.promedioAcumulado").value(4.57));
	}

	@Test
	void muestraLosCreditosAprobadosSobreElTotalDelProgramaYElAvance() throws Exception {
		String token = entrar("historial.avance@ucundinamarca.edu.co");
		elegirPrograma(token);
		sembrarPrimerPeriodo(idDe("historial.avance@ucundinamarca.edu.co"));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.creditosAprobados").value(16))
				.andExpect(jsonPath("$.creditosDelPrograma").value(153))
				// 16 de 153 créditos.
				.andExpect(jsonPath("$.porcentajeDeAvance").value(10.46));
	}

	@Test
	void sinProgramaElegidoHayHistorialPeroNoHayAvance() throws Exception {
		String correo = "historial.sinprograma@ucundinamarca.edu.co";
		String token = entrar(correo);
		sembrarPrimerPeriodo(idDe(correo));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.creditosAprobados").value(16))
				.andExpect(jsonPath("$.creditosDelPrograma").doesNotExist())
				.andExpect(jsonPath("$.porcentajeDeAvance").doesNotExist());
	}

	@Test
	void unaCuentaNuevaVeSuHistorialVacioYNoUnError() throws Exception {
		String token = entrar("historial.vacio@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos.length()").value(0))
				.andExpect(jsonPath("$.creditosAprobados").value(0))
				.andExpect(jsonPath("$.promedioAcumulado").doesNotExist());
	}

	@Test
	void sinSesionNoSeVeNingunHistorial() throws Exception {
		mvc.perform(get("/api/mis/historial")).andExpect(status().isUnauthorized());
	}
}
