package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import java.util.List;

/** Las asignaturas que el estudiante tiene matriculadas, para elegir cuál configurar (SCRUM-27). */
public interface ListarMisAsignaturasMatriculadas {

	/** Todas las que alguna vez matriculó, de cualquier período. */
	List<AsignaturaMatriculada> ejecutar(int idEstudiante);

	/**
	 * Solo las que cursa ahora (SCRUM-86): *Mis notas* no ofrece las de semestres que ya cerraron, que
	 * siguen en el historial. Sin ninguna en curso la lista viene vacía, no es un error.
	 */
	List<AsignaturaMatriculada> delSemestreActual(int idEstudiante);
}
