package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: cambiar la contraseña con el código recibido por correo (RF01). */
public interface RestablecerContrasena {

	void ejecutar(DatosDeRestablecimiento datos);
}
