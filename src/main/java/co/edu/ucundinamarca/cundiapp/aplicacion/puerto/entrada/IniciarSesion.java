package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

/** Caso de uso: iniciar sesión con correo institucional y contraseña (RF01). */
public interface IniciarSesion {

	SesionIniciada ejecutar(String correo, String contrasena, OrigenDeSesion origen);
}
