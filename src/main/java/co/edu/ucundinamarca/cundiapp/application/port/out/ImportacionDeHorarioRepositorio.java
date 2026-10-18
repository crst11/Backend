package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario.Resultado;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import java.util.List;

/** Lo que el núcleo necesita para dejar guardado el horario importado (RF04, SCRUM-81). */
public interface ImportacionDeHorarioRepositorio {

	/** Lo que el estudiante ya tiene en ese período, para decirle qué es nuevo y qué se reemplaza. */
	List<MatriculaConClases> matriculasDe(int idEstudiante, PeriodoAcademico periodo);

	/**
	 * Guarda todo el horario o nada: a medias dejaría una asignatura sin sus clases. Las asignaturas
	 * nuevas quedan en curso; las que ya estaban en curso conservan su matrícula y cambian sus clases.
	 */
	Resultado guardar(int idEstudiante, PeriodoAcademico periodo, List<AsignaturaConHorario> asignaturas);

	record MatriculaConClases(String codigoAsignatura, String estado, int clases) {

		public boolean enCurso() {
			return "en_curso".equals(estado);
		}
	}
}
