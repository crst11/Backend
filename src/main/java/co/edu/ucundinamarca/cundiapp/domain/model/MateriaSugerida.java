package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Una asignatura que el motor propone matricular, con el semestre del plan al que pertenece y la razón (RF13).
 *
 * <p>{@code dependeDeUnaEnCurso} avisa que algún prerrequisito se está cursando ahora: cuenta como cumplido, pero la
 * matrícula solo es válida si el estudiante lo aprueba.
 */
public record MateriaSugerida(
		String codigo,
		String nombre,
		int creditos,
		int periodoSugerido,
		MotivoDeSugerencia motivo,
		boolean dependeDeUnaEnCurso) {
}
