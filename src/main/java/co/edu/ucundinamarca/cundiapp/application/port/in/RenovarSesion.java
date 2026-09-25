package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: cambiar un token de refresco por un acceso nuevo y un refresco nuevo (rotación). */
public interface RenovarSesion {

	SesionIniciada ejecutar(String tokenDeRefresco, OrigenDeSesion origen);
}
