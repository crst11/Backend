package co.edu.ucundinamarca.cundiapp.domain.exception;

/** El token que mandó Google no pasó la verificación: firma, emisor, destinatario o vigencia. */
public class IdentidadExternaInvalidaException extends RuntimeException {

	public IdentidadExternaInvalidaException() {
		super("No pudimos validar tu cuenta de Google. Intenta de nuevo");
	}
}
