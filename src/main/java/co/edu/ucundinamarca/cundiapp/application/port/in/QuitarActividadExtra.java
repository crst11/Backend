package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Quitar una actividad extra del horario (SCRUM-266). Quitar una que no está, o que es de otro, se rechaza igual. */
public interface QuitarActividadExtra {

	void ejecutar(int idEstudiante, int idActividad);
}
