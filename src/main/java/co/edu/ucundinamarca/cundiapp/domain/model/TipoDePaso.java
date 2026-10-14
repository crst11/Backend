package co.edu.ucundinamarca.cundiapp.domain.model;

/** Qué se le pide cargar al estudiante en cada paso del avance (RF03, SCRUM-84). */
public enum TipoDePaso {
	REGISTRO_EXTENDIDO("registro_extendido"),
	HORARIO("horario"),
	/** Las actividades de una asignatura que cursa ahora. */
	ASIGNATURA("asignatura");

	private final String valor;

	TipoDePaso(String valor) {
		this.valor = valor;
	}

	/** Así viaja por la API. */
	public String valor() {
		return valor;
	}
}
