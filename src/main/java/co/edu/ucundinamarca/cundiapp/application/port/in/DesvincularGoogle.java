package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: quitar el inicio con Google; la cuenta sigue entrando con su contraseña. */
public interface DesvincularGoogle {

	void ejecutar(int idEstudiante);
}
