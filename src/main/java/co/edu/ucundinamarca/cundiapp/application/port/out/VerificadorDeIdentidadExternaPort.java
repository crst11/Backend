package co.edu.ucundinamarca.cundiapp.application.port.out;

/**
 * Valida el ID token que entrega un proveedor externo (hoy Google Identity Services) y dice a quién
 * pertenece. Es el punto donde se enchufaría Microsoft Entra ID sin tocar los casos de uso.
 */
public interface VerificadorDeIdentidadExternaPort {

	/**
	 * @throws co.edu.ucundinamarca.cundiapp.domain.exception.IdentidadExternaInvalidaException si el token no es válido
	 * @throws co.edu.ucundinamarca.cundiapp.domain.exception.ServicioExternoNoDisponibleException si el proveedor no responde
	 */
	IdentidadExterna verificar(String idToken);

	/** Quién es la persona según el proveedor: su identificador estable, su correo y si el proveedor lo verificó. */
	record IdentidadExterna(String identificador, String correo, boolean correoVerificado) {
	}
}
