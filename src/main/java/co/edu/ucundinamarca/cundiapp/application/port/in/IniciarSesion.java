package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: iniciar sesión con correo institucional y contraseña (RF01). */
public interface IniciarSesion {

	SesionIniciada ejecutar(String correo, String contrasena, OrigenDeSesion origen);
}
