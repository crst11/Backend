package co.edu.ucundinamarca.cundiapp.domain.model;

/** Con qué se entra a la cuenta. Refleja ck_credencial_proveedor y ck_sesion_proveedor del esquema. */
public enum MetodoDeAcceso {
	LOCAL("local"),
	GOOGLE("google");

	private final String valorEnBd;

	MetodoDeAcceso(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static MetodoDeAcceso desdeBd(String valor) {
		for (MetodoDeAcceso metodo : values()) {
			if (metodo.valorEnBd.equals(valor)) {
				return metodo;
			}
		}
		throw new IllegalArgumentException("Método de acceso desconocido: " + valor);
	}
}
