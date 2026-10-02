package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Para qué sirve un código de 6 dígitos. Los dos usos comparten las mismas reglas (vigencia, intentos
 * y un solo uso), pero no se pueden confundir entre sí: un código pedido para recuperar la contraseña
 * no debe servir para activar una cuenta.
 */
public enum PropositoDelCodigo {

	/** Confirmar que el correo institucional es del estudiante y activar la cuenta (SCRUM-47). */
	VERIFICAR_CORREO("verificar_correo"),

	/** Definir una contraseña nueva sin saber la anterior (SCRUM-68). */
	RECUPERAR_CONTRASENA("recuperar_contrasena");

	private final String valorEnBaseDeDatos;

	PropositoDelCodigo(String valorEnBaseDeDatos) {
		this.valorEnBaseDeDatos = valorEnBaseDeDatos;
	}

	public String valorEnBaseDeDatos() {
		return valorEnBaseDeDatos;
	}

	public static PropositoDelCodigo desdeBaseDeDatos(String valor) {
		for (PropositoDelCodigo proposito : values()) {
			if (proposito.valorEnBaseDeDatos.equals(valor)) {
				return proposito;
			}
		}
		throw new IllegalArgumentException("Propósito de código desconocido: " + valor);
	}
}
