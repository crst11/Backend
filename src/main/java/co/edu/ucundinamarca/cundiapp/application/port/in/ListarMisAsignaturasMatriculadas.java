package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import java.util.List;

/** Las asignaturas que el estudiante tiene matriculadas, para elegir cuál configurar (SCRUM-27). */
public interface ListarMisAsignaturasMatriculadas {

	List<AsignaturaMatriculada> ejecutar(int idEstudiante);
}
