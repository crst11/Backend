package co.edu.ucundinamarca.cundiapp.dominio.excepcion;

/** Correo o contraseña incorrectos. El mensaje es el mismo exista o no el correo. */
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Correo o contraseña incorrectos");
	}
}
