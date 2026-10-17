package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Qué tan comprometida está una asignatura (RF09). El orden de las constantes es el de prioridad:
 * lo más urgente primero, y {@code SIN_DATOS} al final porque no hay nada que decir todavía.
 */
public enum NivelDeRiesgo {
	ALTO,
	MEDIO,
	BAJO,
	SIN_DATOS;

	/**
	 * Si pasar de este nivel a {@code nuevo} es un cambio que se le avisa a la persona (RF10, SCRUM-50): cambió
	 * de verdad y los dos niveles dicen algo. Entrar o salir de «sin datos» no es que la asignatura empeore ni
	 * mejore, es que se empezó (o se dejó) de poder calcular.
	 */
	public boolean seAvisaAlPasarA(NivelDeRiesgo nuevo) {
		return this != nuevo && this != SIN_DATOS && nuevo != SIN_DATOS;
	}

	/** Si pasar de este nivel a {@code nuevo} es empeorar: las constantes van de lo más grave a lo menos. */
	public boolean empeoraAlPasarA(NivelDeRiesgo nuevo) {
		return nuevo.ordinal() < ordinal();
	}
}
