package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.FiltroDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.HorarioSemanal;
import co.edu.ucundinamarca.cundiapp.domain.model.OrdenDelHorario;
import java.util.List;

/**
 * El horario del estudiante: las clases de las asignaturas que está cursando, filtradas y ordenadas
 * como pida (RF04, SCRUM-25 y SCRUM-26).
 */
public interface ConsultarMiHorario {

	Resultado ejecutar(int idEstudiante, FiltroDeHorario filtro, OrdenDelHorario orden);

	/**
	 * {@code sedes} y {@code asignaturas} salen del horario <em>completo</em>, no del filtrado: lo que
	 * se puede elegir tiene que seguir a la vista aunque el filtro actual no deje ninguna clase, o la
	 * persona no tendría cómo quitarlo.
	 */
	record Resultado(
			HorarioSemanal horario,
			List<String> sedes,
			List<AsignaturaDelHorario> asignaturas,
			FiltroDeHorario filtro,
			OrdenDelHorario orden) {
	}
}
