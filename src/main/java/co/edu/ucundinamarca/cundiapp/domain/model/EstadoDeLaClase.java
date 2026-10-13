package co.edu.ucundinamarca.cundiapp.domain.model;

/** Dónde está una clase respecto de la hora de ahora (RF07, SCRUM-32). */
public enum EstadoDeLaClase {
	PASADA("pasada"),
	EN_CURSO("en_curso"),
	/** La siguiente que empieza: la que se destaca para llegar a tiempo al salón. */
	PROXIMA("proxima"),
	LUEGO("luego");

	private final String valor;

	EstadoDeLaClase(String valor) {
		this.valor = valor;
	}

	/** Así viaja por la API. */
	public String valor() {
		return valor;
	}
}
