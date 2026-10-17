package co.edu.ucundinamarca.cundiapp.domain.model;

/** Qué tan importante es un pendiente para quien lo escribió. Refleja la restricción ck_pendiente_prioridad. */
public enum PrioridadDePendiente {
	ALTA("alta"),
	MEDIA("media"),
	BAJA("baja");

	private final String valorEnBd;

	PrioridadDePendiente(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static PrioridadDePendiente desdeBd(String valor) {
		for (PrioridadDePendiente prioridad : values()) {
			if (prioridad.valorEnBd.equals(valor)) {
				return prioridad;
			}
		}
		throw new IllegalArgumentException("Prioridad de pendiente desconocida: " + valor);
	}
}
