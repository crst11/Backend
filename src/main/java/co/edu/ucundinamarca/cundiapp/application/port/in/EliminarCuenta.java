package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Caso de uso: el estudiante elimina su cuenta (RF01). Queda inactiva, sin borrar sus datos. */
public interface EliminarCuenta {

	void ejecutar(int idEstudiante);
}
