package co.edu.ucundinamarca.cundiapp.domain.exception;

/** Correo o contraseña incorrectos. El mensaje es el mismo exista o no el correo. */
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Correo o contraseña incorrectos");
	}
}
