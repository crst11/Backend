package co.edu.ucundinamarca.cundiapp.domain.exception;

/** Las credenciales son correctas pero la cuenta todavía no puede iniciar sesión. */
public class CuentaNoActivaException extends RuntimeException {

	public CuentaNoActivaException(String mensaje) {
		super(mensaje);
	}
}
