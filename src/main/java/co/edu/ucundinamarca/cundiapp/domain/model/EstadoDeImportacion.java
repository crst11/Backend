package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja los valores de la restricción ck_importacion_estado del esquema. */
public enum EstadoDeImportacion {
	PENDIENTE("pendiente"),
	CONFIRMADA("confirmada"),
	REVERTIDA("revertida"),
	FALLIDA("fallida");

	private final String valorEnBd;

	EstadoDeImportacion(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static EstadoDeImportacion desdeBd(String valor) {
		for (EstadoDeImportacion estado : values()) {
			if (estado.valorEnBd.equals(valor)) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de importación desconocido: " + valor);
	}

	/** Solo se puede deshacer una carga que de verdad escribió algo y que no se haya deshecho ya. */
	public boolean sePuedeDeshacer() {
		return this == CONFIRMADA;
	}
}
