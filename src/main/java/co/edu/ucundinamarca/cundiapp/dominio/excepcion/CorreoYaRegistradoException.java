package co.edu.ucundinamarca.cundiapp.dominio.excepcion;

/** Se lanza al intentar registrar un correo institucional que ya tiene cuenta. */
public class CorreoYaRegistradoException extends RuntimeException {

	public CorreoYaRegistradoException(String mensaje) {
		super(mensaje);
	}
}
