package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja ck_recurso_tipo del esquema: un documento, un formato o plantilla, o un enlace a una página. */
public enum TipoDeRecurso {
	DOCUMENTO("documento"),
	FORMATO("formato"),
	ENLACE("enlace");

	private final String valorEnBd;

	TipoDeRecurso(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static TipoDeRecurso desdeBd(String valor) {
		for (TipoDeRecurso tipo : values()) {
			if (tipo.valorEnBd.equals(valor)) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Tipo de recurso desconocido: " + valor);
	}
}
