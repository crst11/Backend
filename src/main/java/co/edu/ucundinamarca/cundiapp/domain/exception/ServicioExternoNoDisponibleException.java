package co.edu.ucundinamarca.cundiapp.domain.exception;

/** Un servicio de terceros (Google) no respondió o no está configurado: no es culpa de la persona. */
public class ServicioExternoNoDisponibleException extends RuntimeException {

	public ServicioExternoNoDisponibleException(String mensaje) {
		super(mensaje);
	}

	public ServicioExternoNoDisponibleException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
