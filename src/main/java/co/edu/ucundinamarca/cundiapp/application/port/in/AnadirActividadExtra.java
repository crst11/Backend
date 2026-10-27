package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.ActividadExtra;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import java.time.LocalTime;
import java.util.List;

/**
 * Añadir una actividad extra de la universidad al horario (SCRUM-266): una clase de baile, un semillero, deporte.
 * No es una asignatura. Una fila por día elegido, todas o ninguna.
 */
public interface AnadirActividadExtra {

	/** Las actividades guardadas, una por día, ya con su número. */
	List<ActividadExtra> ejecutar(int idEstudiante, DatosDeLaActividad datos);

	/** El lugar y el responsable son opcionales. */
	record DatosDeLaActividad(
			String nombre, List<DiaDeLaSemana> dias, LocalTime horaInicio, LocalTime horaFin, String lugar,
			String responsable) {
	}
}
