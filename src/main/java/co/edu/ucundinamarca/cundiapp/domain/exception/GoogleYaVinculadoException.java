package co.edu.ucundinamarca.cundiapp.domain.exception;

/** La vinculación choca con otra que ya existe: la de otra cuenta o la que el estudiante ya tiene. */
public class GoogleYaVinculadoException extends RuntimeException {

	public GoogleYaVinculadoException(String mensaje) {
		super(mensaje);
	}
}
