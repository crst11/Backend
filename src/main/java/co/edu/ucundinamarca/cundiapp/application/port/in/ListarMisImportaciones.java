package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;
import java.util.List;

/** El historial de cargas del estudiante, de la más reciente a la más antigua (RF03, SCRUM-24). */
public interface ListarMisImportaciones {

	List<Importacion> ejecutar(int idEstudiante);
}
