package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja ck_recurso_estado del esquema. Un recurso retirado ya no se muestra en la guía. */
public enum EstadoDeRecurso {
	VIGENTE("vigente"),
	PENDIENTE_REVISION("pendiente_revision"),
	RETIRADO("retirado");

	private final String valorEnBd;

	EstadoDeRecurso(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static EstadoDeRecurso desdeBd(String valor) {
		for (EstadoDeRecurso estado : values()) {
			if (estado.valorEnBd.equals(valor)) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de recurso desconocido: " + valor);
	}
}
