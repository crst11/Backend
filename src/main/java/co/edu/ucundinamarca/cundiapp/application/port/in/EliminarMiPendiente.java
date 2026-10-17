package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Borrar un pendiente (RF06, SCRUM-29). Uno que no existe o es de otro estudiante responde lo mismo. */
public interface EliminarMiPendiente {

	void ejecutar(int idEstudiante, int idPendiente);
}
