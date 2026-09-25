package co.edu.ucundinamarca.cundiapp.domain.exception;

/** Se lanza al intentar registrar un correo institucional que ya tiene cuenta. */
public class CorreoYaRegistradoException extends RuntimeException {

	public CorreoYaRegistradoException(String mensaje) {
		super(mensaje);
	}
}
