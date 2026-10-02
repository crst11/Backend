package co.edu.ucundinamarca.cundiapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La ruta de aprendizaje cargada en la migración V6 tiene que coincidir con lo que publica la
 * universidad (SCRUM-21). Si alguien la edita y se equivoca en un crédito, estas pruebas lo cazan.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PlanDeEstudiosTest {

	private static final String PLAN = "ISC-2020-FUSA";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void elProgramaEsElDeLaPaginaOficial() {
		Map<String, Object> programa = jdbc.queryForMap(
				"SELECT nombre_programa, sede, total_creditos, numero_periodos FROM cundiapp.programa_academico"
						+ " WHERE codigo_programa = '109964'");

		assertThat(programa.get("nombre_programa")).isEqualTo("Ingeniería de Sistemas y Computación");
		assertThat(programa.get("sede")).isEqualTo("Fusagasugá");
		assertThat(programa.get("total_creditos")).isEqualTo(153);
		assertThat(programa.get("numero_periodos")).isEqualTo(9);
	}

	@Test
	void losCreditosDelPlanSumanLos153QueExigeElPrograma() {
		Integer suma = jdbc.queryForObject(
				"SELECT sum(creditos) FROM cundiapp.asignatura WHERE codigo_plan = ?", Integer.class, PLAN);

		assertThat(suma).isEqualTo(153);
	}

	@Test
	void cadaPeriodoTieneLosCreditosDeLaRutaDeAprendizaje() {
		List<Map<String, Object>> porPeriodo = jdbc.queryForList(
				"SELECT periodo_sugerido, sum(creditos) AS creditos FROM cundiapp.asignatura"
						+ " WHERE codigo_plan = ? GROUP BY periodo_sugerido ORDER BY periodo_sugerido", PLAN);

		// 16, 18, 17, 16, 17, 18, 17, 18, 16: los totales que trae la ruta al pie de cada columna.
		assertThat(porPeriodo).extracting(fila -> ((Number) fila.get("creditos")).intValue())
				.containsExactly(16, 18, 17, 16, 17, 18, 17, 18, 16);
	}

	@Test
	void todaAsignaturaQuedaEnUnPeriodoDelUnoAlNueve() {
		Integer fuera = jdbc.queryForObject(
				"SELECT count(*) FROM cundiapp.asignatura WHERE codigo_plan = ?"
						+ " AND (periodo_sugerido IS NULL OR periodo_sugerido NOT BETWEEN 1 AND 9)", Integer.class, PLAN);

		assertThat(fuera).isZero();
	}

	@Test
	void lasDeDiagnosticoYNivelatorioNoPonderan() {
		// En Academusoft valen 0: se cursan, pero no cuentan para el promedio ni para los créditos.
		List<Map<String, Object>> conCreditos = jdbc.queryForList(
				"SELECT codigo_asignatura FROM cundiapp.asignatura"
						+ " WHERE codigo_plan = ? AND codigo_asignatura LIKE 'DN-%' AND creditos <> 0", PLAN);

		assertThat(conCreditos).isEmpty();
	}

	@Test
	void ningunPrerrequisitoSeCursaDespuesDeLaAsignaturaQueLoExige() {
		// Un requisito que va en un período posterior sería imposible de cumplir: señal de transcripción mala.
		List<Map<String, Object>> alReves = jdbc.queryForList("""
				SELECT p.codigo_asignatura, p.codigo_requerida
				  FROM cundiapp.prerrequisito p
				  JOIN cundiapp.asignatura a ON a.codigo_asignatura = p.codigo_asignatura
				  JOIN cundiapp.asignatura r ON r.codigo_asignatura = p.codigo_requerida
				 WHERE r.periodo_sugerido >= a.periodo_sugerido""");

		assertThat(alReves).isEmpty();
	}

	@Test
	void losRequisitosConocidosQuedaronCargados() {
		assertThat(requisitosDe("CAD612021209")).containsExactly("CAD612021102");
		assertThat(requisitosDe("CAD612021521")).containsExactlyInAnyOrder("CAD612021313", "CAD612021416");
		// Cátedra Generación Siglo 21 exige las nueve del período anterior: es la de más requisitos.
		assertThat(requisitosDe("CAI1002020609")).hasSize(9);
		// Álgebra Lineal abre el plan: no depende de nada.
		assertThat(requisitosDe("CAD612021101")).isEmpty();
	}

	private List<String> requisitosDe(String codigo) {
		return jdbc.queryForList(
				"SELECT codigo_requerida FROM cundiapp.prerrequisito WHERE codigo_asignatura = ?",
				String.class, codigo);
	}
}
