package co.edu.ucundinamarca.cundiapp.dominio.excepcion;

/** Se superó el número de intentos fallidos seguidos permitido. */
public class DemasiadosIntentosException extends RuntimeException {

	public DemasiadosIntentosException() {
		super("Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo");
	}
}
