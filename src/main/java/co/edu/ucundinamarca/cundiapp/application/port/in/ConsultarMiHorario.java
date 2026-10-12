package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.HorarioSemanal;

/** El horario del estudiante: las clases de las asignaturas que está cursando (RF04, SCRUM-25). */
public interface ConsultarMiHorario {

	HorarioSemanal ejecutar(int idEstudiante);
}
