package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Refleja los valores de la restricción ck_categoria_origen del esquema.
 *
 * <p>Distingue lo que venía puesto por la plantilla de lo que el estudiante armó: así la pantalla
 * puede decirle que todavía está con los cortes por defecto.
 */
public enum OrigenDeCategoria {
	PLANTILLA("plantilla"),
	ESTUDIANTE("estudiante");

	private final String valorEnBd;

	OrigenDeCategoria(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static OrigenDeCategoria desdeBd(String valor) {
		for (OrigenDeCategoria origen : values()) {
			if (origen.valorEnBd.equals(valor)) {
				return origen;
			}
		}
		throw new IllegalArgumentException("Origen de categoría desconocido: " + valor);
	}
}
