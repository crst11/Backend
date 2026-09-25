package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: cerrar la sesión de este dispositivo. Es idempotente: cerrar dos veces no falla. */
public interface CerrarSesion {

	void ejecutar(String tokenDeRefresco);
}
