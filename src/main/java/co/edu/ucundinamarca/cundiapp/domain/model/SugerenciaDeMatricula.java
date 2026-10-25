package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/**
 * Lo que el motor propone matricular el próximo semestre (RF13, SCRUM-244).
 *
 * <p>{@code semestreActual} es nulo si el estudiante no cursa nada ahora, y {@code semestreSugerido} es nulo si no queda
 * nada por matricular. Los créditos se suman sobre {@code materias}; las adelantables son opcionales y no cuentan.
 */
public record SugerenciaDeMatricula(
		Integer semestreActual,
		Integer semestreSugerido,
		List<MateriaSugerida> materias,
		List<MateriaSugerida> adelantables) {

	public SugerenciaDeMatricula {
		materias = materias == null ? List.of() : List.copyOf(materias);
		adelantables = adelantables == null ? List.of() : List.copyOf(adelantables);
	}

	public int creditos() {
		return materias.stream().mapToInt(MateriaSugerida::creditos).sum();
	}
}
