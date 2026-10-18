package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Cómo está un pendiente frente a la hora de ahora (RF06, SCRUM-30). La calcula el dominio y la pantalla
 * solo la pinta: el estudiante no recibe una fecha para que el teléfono decida qué es urgente.
 */
public enum SituacionDelPendiente {
	/** Ya lo cumplió. */
	CUMPLIDO,
	/** Se pasó la fecha límite y sigue abierto. */
	VENCIDO,
	/** Vence en menos de 48 horas. */
	URGENTE,
	/** Todavía hay tiempo. */
	PROXIMO
}
