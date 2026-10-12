package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.Docente;
import co.edu.ucundinamarca.cundiapp.domain.model.EspacioFisico;
import java.time.LocalTime;

/**
 * Agregar a mano un bloque del horario de una asignatura que el estudiante está cursando (RF04,
 * SCRUM-25). La otra forma de llenarlo, el reporte en PDF, llega con su propia historia.
 */
public interface AgregarClaseAlHorario {

	ClaseDelHorario ejecutar(int idEstudiante, int idMatricula, DatosDeLaClase datos);

	/** El lugar y el docente pueden venir nulos: se puede conocer la hora antes que el salón. */
	record DatosDeLaClase(
			DiaDeLaSemana dia, LocalTime horaInicio, LocalTime horaFin, EspacioFisico espacio, Docente docente) {
	}
}
