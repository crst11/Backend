package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El motor académico (SCRUM-22). Es la prueba estrella del Sprint 2: alimentado con un Registro
 * Académico Extendido real, tiene que reproducir los créditos y los promedios que calcula la
 * universidad.
 *
 * <p>Sobre la tolerancia de los promedios: Academusoft publica las notas con un decimal, pero por
 * dentro las guarda con dos (el esquema del proyecto usa NUMERIC(3,2) por la misma razón). Con los
 * datos del reporte es imposible reproducir el último decimal, y se nota: en 2024-2 el acumulado
 * oficial trunca (4.456 → 4.4) y en 2025-2 redondea (4.575 → 4.6). Ninguna regla explica las dos.
 *
 * <p>La tolerancia no es un número a ojo: son dos redondeos que se suman. Cada nota que leemos
 * puede estar hasta 0.05 lejos de la que la universidad tiene guardada, y el promedio que el
 * reporte imprime puede estar otros 0.05 lejos del que calculó. De ahí 0.1, y no menos. Los
 * créditos, que son enteros y no pasan por ningún redondeo, se comprueban exactos.
 */
class HistorialAcademicoTest {

	/** Los dos redondeos que el reporte acumula: el de cada nota y el del promedio impreso. */
	private static final BigDecimal LOS_DOS_REDONDEOS = new BigDecimal("0.1");

	private static AsignaturaCursada cursada(String codigo, int creditos, String nota) {
		return new AsignaturaCursada(codigo, codigo, creditos, new BigDecimal(nota), EstadoDeAsignatura.APROBADA);
	}

	/** Las de diagnóstico y nivelatorio: se cursan y se califican, pero valen 0 créditos. */
	private static AsignaturaCursada nivelatoria(String codigo, String nota) {
		return cursada(codigo, 0, nota);
	}

	// Datos del Registro Académico Extendido del 7 de septiembre de 2026. Sin datos personales:
	// solo los códigos oficiales de las asignaturas, sus créditos y sus notas definitivas.
	private static PeriodoCursado periodo2024_2() {
		return new PeriodoCursado("2024-2", List.of(
				cursada("CAD612021101", 3, "4.2"),
				nivelatoria("DN-CAI1002020202", "4.0"),
				nivelatoria("DN-CAI1002020201", "5.0"),
				cursada("CAD612021106", 4, "4.3"),
				cursada("CAD612021103", 2, "4.3"),
				cursada("CAD612021105", 2, "4.6"),
				cursada("CAD612021102", 3, "4.7"),
				cursada("CAD612021104", 2, "4.8")));
	}

	private static PeriodoCursado periodo2025_1() {
		return new PeriodoCursado("2025-1", List.of(
				cursada("CAD612021207", 4, "5.0"),
				cursada("CAI1002020202", 2, "4.6"),
				nivelatoria("DN-CAI1002020303", "4.2"),
				nivelatoria("DN-CAI1002020304", "4.7"),
				cursada("CAD612021210", 3, "5.0"),
				cursada("CAD612021208", 4, "4.9"),
				cursada("CAD612021209", 3, "3.9"),
				cursada("CAI1002020201", 2, "4.9")));
	}

	private static PeriodoCursado periodo2025_2() {
		return new PeriodoCursado("2025-2", List.of(
				cursada("CAD612021311", 4, "4.3"),
				cursada("CAI1002020303", 2, "4.8"),
				cursada("CAI1002020305", 2, "4.8"),
				nivelatoria("DN-CAI1002020613", "5.0"),
				nivelatoria("DN-CAI1002020612", "4.4"),
				cursada("CAD612021312", 4, "4.2"),
				cursada("CAI1002020304", 2, "4.7"),
				cursada("CAD612021313", 3, "4.7")));
	}

	private static PeriodoCursado periodo2026_1() {
		return new PeriodoCursado("2026-1", List.of(
				cursada("CAD612021416", 4, "4.6"),
				cursada("CAD612021414", 3, "4.8"),
				cursada("CAD612021417", 2, "4.7"),
				cursada("CAD612021415", 3, "4.6"),
				cursada("CAI1002020406", 2, "4.8"),
				cursada("CAD612021418", 2, "4.7")));
	}

	private static HistorialAcademico historialReal() {
		return new HistorialAcademico(List.of(periodo2024_2(), periodo2025_1(), periodo2025_2(), periodo2026_1()));
	}

	@Test
	void losCreditosDeCadaPeriodoCoincidenConElReporteOficial() {
		// Ponderación matriculada del reporte: 16, 18, 17 y 16.
		assertThat(historialReal().periodos()).extracting(PeriodoCursado::creditosMatriculados)
				.containsExactly(16, 18, 17, 16);
	}

	@Test
	void losCreditosAprobadosSumanLosDelReporteOficial() {
		// "Total de Créditos Aprobados: 67".
		assertThat(historialReal().creditosAprobados()).isEqualTo(67);
	}

	@Test
	void elPromedioDeCadaPeriodoCoincideConElReporteOficial() {
		// Promedio Período del reporte: 4.4, 4.7, 4.5 y 4.7.
		List<String> oficiales = List.of("4.4", "4.7", "4.5", "4.7");
		List<PeriodoCursado> periodos = historialReal().periodos();

		for (int i = 0; i < periodos.size(); i++) {
			assertThat(periodos.get(i).promedio().orElseThrow())
					.as("promedio de %s", periodos.get(i).codigo())
					.isCloseTo(new BigDecimal(oficiales.get(i)), within(LOS_DOS_REDONDEOS));
		}
	}

	@Test
	void elPromedioAcumuladoCoincideConElReporteOficial() {
		// "Promedio Acumulado: 4.6".
		assertThat(historialReal().promedioAcumulado().orElseThrow())
				.isCloseTo(new BigDecimal("4.6"), within(LOS_DOS_REDONDEOS));
	}

	@Test
	void elAcumuladoHastaCadaPeriodoSigueAlReporteOficial() {
		// Promedio Acumulado por fila del reporte: 4.4, 4.6, 4.6 y 4.6.
		List<String> oficiales = List.of("4.4", "4.6", "4.6", "4.6");
		var historial = historialReal();

		for (int i = 0; i < oficiales.size(); i++) {
			String periodo = historial.periodos().get(i).codigo();
			assertThat(historial.promedioAcumuladoHasta(periodo).orElseThrow())
					.as("acumulado hasta %s", periodo)
					.isCloseTo(new BigDecimal(oficiales.get(i)), within(LOS_DOS_REDONDEOS));
		}
	}

	@Test
	void lasDeCeroCreditosNoMuevenElPromedio() {
		// Son las de diagnóstico y nivelatorio: se cursan y se califican, pero no ponderan.
		var conNivelatorias = new PeriodoCursado("2024-2", List.of(
				cursada("A", 3, "3.0"), nivelatoria("DN-X", "5.0"), nivelatoria("DN-Y", "1.0")));
		var sinNivelatorias = new PeriodoCursado("2024-2", List.of(cursada("A", 3, "3.0")));

		assertThat(conNivelatorias.promedio()).isEqualTo(sinNivelatorias.promedio());
	}

	@Test
	void elPromedioPondera_noEsElPromedioSimpleDeLasNotas() {
		// 4 créditos con 5.0 y 1 con 1.0: ponderado da 4.2; el promedio simple daría 3.0.
		var periodo = new PeriodoCursado("2024-2", List.of(cursada("A", 4, "5.0"), cursada("B", 1, "1.0")));

		assertThat(periodo.promedio().orElseThrow()).isEqualByComparingTo(new BigDecimal("4.20"));
	}

	@Test
	void unaAsignaturaEnCursoNoEntraAlPromedioNiALosCreditosAprobados() {
		// Mientras no tenga nota definitiva no hay nada que promediar: contarla como 0 sería mentir.
		var enCurso = new AsignaturaCursada("B", "B", 4, null, EstadoDeAsignatura.EN_CURSO);
		var periodo = new PeriodoCursado("2026-2", List.of(cursada("A", 3, "4.0"), enCurso));

		assertThat(periodo.promedio().orElseThrow()).isEqualByComparingTo(new BigDecimal("4.00"));
		assertThat(periodo.creditosAprobados()).isEqualTo(3);
		assertThat(periodo.creditosMatriculados()).isEqualTo(7);
	}

	@Test
	void unaAsignaturaCanceladaNoCuentaEnNingunLado() {
		var cancelada = new AsignaturaCursada("B", "B", 4, null, EstadoDeAsignatura.CANCELADA);
		var periodo = new PeriodoCursado("2026-2", List.of(cursada("A", 3, "4.0"), cancelada));

		assertThat(periodo.creditosMatriculados()).isEqualTo(3);
		assertThat(periodo.creditosAprobados()).isEqualTo(3);
	}

	@Test
	void unaReprobadaBajaElPromedioPeroNoSumaCreditosAprobados() {
		var reprobada = new AsignaturaCursada("B", "B", 3, new BigDecimal("2.0"), EstadoDeAsignatura.REPROBADA);
		var periodo = new PeriodoCursado("2026-2", List.of(cursada("A", 3, "4.0"), reprobada));

		assertThat(periodo.promedio().orElseThrow()).isEqualByComparingTo(new BigDecimal("3.00"));
		assertThat(periodo.creditosAprobados()).isEqualTo(3);
		assertThat(periodo.creditosMatriculados()).isEqualTo(6);
	}

	@Test
	void unPeriodoSinNingunaNotaNoTienePromedio() {
		// Recién matriculado: la pantalla dirá que todavía no hay promedio, no un 0.0.
		var periodo = new PeriodoCursado("2026-2",
				List.of(new AsignaturaCursada("A", "A", 3, null, EstadoDeAsignatura.EN_CURSO)));

		assertThat(periodo.promedio()).isEmpty();
	}

	@Test
	void elAvanceEsLosCreditosAprobadosSobreLosDelPrograma() {
		// 67 de 153 créditos del programa.
		assertThat(historialReal().porcentajeDeAvance(153)).isEqualByComparingTo(new BigDecimal("43.79"));
	}

	@Test
	void elAvanceNoSePuedeCalcularContraUnProgramaSinCreditos() {
		assertThatThrownBy(() -> historialReal().porcentajeDeAvance(0))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void losPeriodosQuedanEnOrdenCronologicoAunqueLleguenDesordenados() {
		var historial = new HistorialAcademico(List.of(periodo2025_2(), periodo2024_2(), periodo2025_1()));

		assertThat(historial.periodos()).extracting(PeriodoCursado::codigo)
				.containsExactly("2024-2", "2025-1", "2025-2");
	}

	@Test
	void unaNotaFueraDeLaEscalaNoSeAcepta() {
		assertThatThrownBy(() -> cursada("A", 3, "5.5"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void losCreditosNegativosNoSeAceptan() {
		assertThatThrownBy(() -> cursada("A", -1, "4.0"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void unHistorialVacioNoTienePromedioNiAvance() {
		var vacio = new HistorialAcademico(List.of());

		assertThat(vacio.promedioAcumulado()).isEmpty();
		assertThat(vacio.creditosAprobados()).isZero();
		assertThat(vacio.porcentajeDeAvance(153)).isEqualByComparingTo(BigDecimal.ZERO);
	}
}
