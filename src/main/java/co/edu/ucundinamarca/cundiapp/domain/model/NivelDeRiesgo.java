package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Qué tan comprometida está una asignatura (RF09). El orden de las constantes es el de prioridad:
 * lo más urgente primero, y {@code SIN_DATOS} al final porque no hay nada que decir todavía.
 */
public enum NivelDeRiesgo {
	ALTO,
	MEDIO,
	BAJO,
	SIN_DATOS
}
