package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Dónde está el estudiante respecto de su meta en una asignatura (RF08, SCRUM-33).
 */
public enum SituacionDeLaMeta {
	/** Con lo que ya lleva alcanza la meta, pase lo que pase en lo que falta. */
	ALCANZADA,
	/** Todavía no la alcanza, pero con una nota de hasta 5.0 en lo que falta sí puede. */
	ALCANZABLE,
	/** Ni con 5.0 en todo lo que falta llegaría, o ya no queda nada por evaluar. */
	INALCANZABLE
}
