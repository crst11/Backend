package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Ver mis asignaturas cursadas, mis promedios y mi avance en la carrera (RF02, SCRUM-22). */
public interface ConsultarMiHistorial {

	MiHistorial ejecutar(int idEstudiante);
}
