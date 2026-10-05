package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja los valores de la restricción ck_actividad_estado del esquema. */
public enum EstadoDeEntrega {
	NO_ENTREGADA("no_entregada"),
	SIN_CALIFICAR("sin_calificar"),
	CALIFICADA("calificada");

	private final String valorEnBd;

	EstadoDeEntrega(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static EstadoDeEntrega desdeBd(String valor) {
		for (EstadoDeEntrega estado : values()) {
			if (estado.valorEnBd.equals(valor)) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de entrega desconocido: " + valor);
	}
}
