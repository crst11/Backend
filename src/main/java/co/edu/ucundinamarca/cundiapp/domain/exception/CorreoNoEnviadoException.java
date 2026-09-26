package co.edu.ucundinamarca.cundiapp.domain.exception;

/** El código de verificación no se pudo entregar al correo: la falla es del canal, no de la persona. */
public class CorreoNoEnviadoException extends RuntimeException {

	public CorreoNoEnviadoException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
