package co.edu.ucundinamarca.cundiapp.domain.model;

/** Qué clase de día es una fecha para quien estudia (RF07, SCRUM-85). */
public enum TipoDeDia {
	/** Dentro de un período académico y no festivo: puede haber clase. */
	LECTIVO("lectivo"),
	FESTIVO("festivo"),
	/** Entre dos semestres: no hay clase aunque el horario tenga ese día de la semana. */
	FUERA_DE_PERIODO("fuera_de_periodo");

	private final String valor;

	TipoDeDia(String valor) {
		this.valor = valor;
	}

	/** Así viaja por la API. */
	public String valor() {
		return valor;
	}
}
