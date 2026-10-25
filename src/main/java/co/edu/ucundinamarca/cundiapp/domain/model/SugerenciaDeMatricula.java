package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/**
 * Lo que el motor propone matricular el próximo semestre (RF13, SCRUM-244).
 *
 * <p>{@code semestreActual} es nulo si el estudiante no cursa nada ahora, y {@code semestreSugerido} es nulo si no queda
 * nada por matricular. Los créditos se suman sobre {@code materias}; las adelantables son opcionales y no cuentan.
 * {@code creditosDelSemestre} es lo que el plan de estudios asigna al semestre sugerido (cero si no hay sugerencia): el
 * valor con el que arranca la persona al calcular cuántos créditos le sobran, que ella puede cambiar.
 */
public record SugerenciaDeMatricula(
		Integer semestreActual,
		Integer semestreSugerido,
		List<MateriaSugerida> materias,
		List<MateriaSugerida> adelantables,
		int creditosDelSemestre) {

	public SugerenciaDeMatricula {
		materias = materias == null ? List.of() : List.copyOf(materias);
		adelantables = adelantables == null ? List.of() : List.copyOf(adelantables);
	}

	public int creditos() {
		return materias.stream().mapToInt(MateriaSugerida::creditos).sum();
	}
}
