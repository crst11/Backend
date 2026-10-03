package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.HistorialAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.ResumenOficialDePeriodo;
import java.util.List;

/** Lo que el núcleo necesita saber del historial cursado del estudiante (RF02). */
public interface HistorialAcademicoRepositorio {

	HistorialAcademico historialDe(int idEstudiante);

	/** Los promedios que trajo el reporte oficial, si el estudiante ya importó alguno. */
	List<ResumenOficialDePeriodo> resumenesOficialesDe(int idEstudiante);
}
