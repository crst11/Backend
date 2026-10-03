package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja los valores de la restricción ck_asignatura_tipo del esquema. */
public enum TipoDeAsignatura {
	OBLIGATORIA("obligatoria"),
	ELECTIVA("electiva"),
	PROFUNDIZACION("profundizacion");

	private final String valorEnBd;

	TipoDeAsignatura(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static TipoDeAsignatura desdeBd(String valor) {
		for (TipoDeAsignatura tipo : values()) {
			if (tipo.valorEnBd.equals(valor)) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Tipo de asignatura desconocido: " + valor);
	}
}
