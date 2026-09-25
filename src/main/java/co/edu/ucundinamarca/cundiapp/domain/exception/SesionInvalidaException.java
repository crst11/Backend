package co.edu.ucundinamarca.cundiapp.domain.exception;

/** El token de refresco no sirve: no existe, venció, se cerró o ya se había usado. */
public class SesionInvalidaException extends RuntimeException {

	public SesionInvalidaException() {
		super("La sesión no es válida o expiró. Inicia sesión de nuevo");
	}
}
