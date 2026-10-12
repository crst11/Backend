package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlanDeEstudiosTest {

	private static final ProgramaAcademico PROGRAMA =
			new ProgramaAcademico("109964", "Ingeniería de Sistemas", "Ingeniería", "Fusagasugá", 153, 9);

	private static Asignatura asignatura(String codigo, String nombre, int creditos, Integer periodo, String... previas) {
		return new Asignatura(codigo, nombre, creditos, TipoDeAsignatura.OBLIGATORIA, periodo, List.of(previas));
	}

	@Test
	void agrupaPorPeriodoDeMenorAMayorYAlfabeticoDentroDeCadaUno() {
		var plan = new PlanDeEstudios("P", PROGRAMA, List.of(
				asignatura("B", "Programación I", 3, 2),
				asignatura("A", "Álgebra Lineal", 3, 1),
				asignatura("C", "Cálculo Diferencial", 4, 2)));

		assertThat(plan.porPeriodo().keySet()).containsExactly(1, 2);
		assertThat(plan.porPeriodo().get(2)).extracting(Asignatura::nombre)
				.containsExactly("Cálculo Diferencial", "Programación I");
	}

	@Test
	void unaInicialConTildeNoSeVaAlFinalDeLaLista() {
		// Comparando por código de carácter la "Á" cae después de la "D" y el estudiante vería la lista al revés.
		var plan = new PlanDeEstudios("P", PROGRAMA, List.of(
				asignatura("C", "Diagnóstico y Nivelatorio Comunicación", 0, 1),
				asignatura("A", "Álgebra Lineal", 3, 1),
				asignatura("B", "Ética Profesional", 2, 1)));

		assertThat(plan.porPeriodo().get(1)).extracting(Asignatura::nombre)
				.containsExactly("Álgebra Lineal", "Diagnóstico y Nivelatorio Comunicación", "Ética Profesional");
	}

	@Test
	void sumaLosCreditosDelPlanYDeCadaPeriodo() {
		var plan = new PlanDeEstudios("P", PROGRAMA, List.of(
				asignatura("A", "Álgebra Lineal", 3, 1),
				asignatura("B", "Física I", 4, 1),
				asignatura("C", "Programación I", 3, 2)));

		assertThat(plan.creditosTotales()).isEqualTo(10);
		assertThat(plan.creditosDelPeriodo(1)).isEqualTo(7);
		assertThat(plan.creditosDelPeriodo(9)).isZero();
	}

	@Test
	void unaAsignaturaSinPeriodoNoApareceEnElAgrupado() {
		// El esquema permite período nulo; una electiva sin ubicar no debe romper la pantalla.
		var plan = new PlanDeEstudios("P", PROGRAMA, List.of(
				asignatura("A", "Álgebra Lineal", 3, 1),
				asignatura("X", "Electiva libre", 3, null)));

		assertThat(plan.porPeriodo()).hasSize(1);
		// Pero sí cuenta para el total de créditos del plan.
		assertThat(plan.creditosTotales()).isEqualTo(6);
	}

	@Test
	void lasDeDiagnosticoNoPonderan() {
		var nivelatoria = asignatura("DN-1", "Diagnóstico y Nivelatorio", 0, 1);
		var normal = asignatura("A", "Álgebra Lineal", 3, 1);

		assertThat(nivelatoria.pondera()).isFalse();
		assertThat(normal.pondera()).isTrue();
	}

	@Test
	void cadaAsignaturaConservaSusPrerrequisitos() {
		var plan = new PlanDeEstudios("P", PROGRAMA, List.of(
				asignatura("B", "Programación I", 3, 2, "A")));

		assertThat(plan.porPeriodo().get(2).get(0).prerrequisitos()).containsExactly("A");
	}

	@Test
	void laListaDeAsignaturasNoSePuedeModificarPorFuera() {
		var original = new java.util.ArrayList<>(List.of(asignatura("A", "Álgebra Lineal", 3, 1)));
		var plan = new PlanDeEstudios("P", PROGRAMA, original);

		original.clear();

		assertThat(plan.asignaturas()).hasSize(1);
	}
}
