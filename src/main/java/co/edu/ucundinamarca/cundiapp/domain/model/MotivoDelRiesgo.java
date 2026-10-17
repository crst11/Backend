package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Por qué una asignatura quedó en su nivel (RF09, SCRUM-35: «explica por qué»). Lo decide el dominio;
 * la frase que lee el estudiante se arma en la capa de presentación a partir de esto y de los números.
 */
public enum MotivoDelRiesgo {
	/** Ni con 5.0 en todo lo que falta se llega a la meta. */
	INALCANZABLE,
	/** La nota que hace falta pasa del umbral alto. */
	SOBRE_EL_UMBRAL_ALTO,
	/** La nota que hace falta pasa del umbral medio, sin llegar al alto. */
	SOBRE_EL_UMBRAL_MEDIO,
	/** La nota que hace falta no pasa del umbral medio. */
	DENTRO_DE_LO_ALCANZABLE,
	/** Lo que ya lleva alcanza la meta. */
	META_ALCANZADA,
	/** Todavía no dijo cómo lo evalúan en esa asignatura. */
	SIN_ESTRUCTURA
}
