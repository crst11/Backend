package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/**
 * Una asignatura que el motor propone matricular, con el semestre del plan al que pertenece y la razón (RF13).
 *
 * <p>{@code dependeDeUnaEnCurso} avisa que algún prerrequisito se está cursando ahora: cuenta como cumplido, pero la
 * matrícula solo es válida si el estudiante lo aprueba. {@code prerrequisitos} son los nombres de las materias que pide
 * el plan; vacío si no pide ninguna.
 */
public record MateriaSugerida(
		String codigo,
		String nombre,
		int creditos,
		int periodoSugerido,
		MotivoDeSugerencia motivo,
		boolean dependeDeUnaEnCurso,
		List<String> prerrequisitos) {

	public MateriaSugerida {
		prerrequisitos = prerrequisitos == null ? List.of() : List.copyOf(prerrequisitos);
	}
}
