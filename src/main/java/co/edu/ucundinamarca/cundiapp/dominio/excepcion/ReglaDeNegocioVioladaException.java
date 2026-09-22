package co.edu.ucundinamarca.cundiapp.dominio.excepcion;

/** Se lanza cuando una operación del dominio viola una regla de negocio. */
public class ReglaDeNegocioVioladaException extends RuntimeException {

	public ReglaDeNegocioVioladaException(String mensaje) {
		super(mensaje);
	}
}
