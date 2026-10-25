package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.SugerenciaDeMatricula;

/**
 * La sugerencia de matrícula, o la razón por la que no hay una (RF13, SCRUM-244).
 *
 * @param sugerencia nula si no se pudo calcular ({@code SIN_PLAN} y {@code SIN_HISTORIAL})
 * @param razon nula si hay sugerencia con materias
 */
public record SugerenciaParaMatricular(SugerenciaDeMatricula sugerencia, Razon razon) {

	/** Por qué no hay materias que sugerir. */
	public enum Razon {
		/** Todavía no eligió un plan de estudios. */
		SIN_PLAN,
		/** Todavía no importó lo que lleva cursado. */
		SIN_HISTORIAL,
		/** Ya aprobó o cursa todo lo que el plan pide. */
		NADA_POR_MATRICULAR
	}
}
