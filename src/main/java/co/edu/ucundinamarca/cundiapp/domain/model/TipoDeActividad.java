package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja los valores de la restricción ck_actividad_tipo del esquema. */
public enum TipoDeActividad {
	PARCIAL("parcial"),
	TALLER("taller"),
	QUIZ("quiz"),
	EXPOSICION("exposicion");

	private final String valorEnBd;

	TipoDeActividad(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static TipoDeActividad desdeBd(String valor) {
		for (TipoDeActividad tipo : values()) {
			if (tipo.valorEnBd.equals(valor)) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Tipo de actividad desconocido: " + valor);
	}
}
