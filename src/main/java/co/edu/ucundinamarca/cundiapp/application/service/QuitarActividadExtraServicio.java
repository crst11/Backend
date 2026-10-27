package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarActividadExtra;
import co.edu.ucundinamarca.cundiapp.application.port.out.ActividadExtraRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Quita una actividad extra (SCRUM-266). Una que no existe y una de otro estudiante dan la misma respuesta: decir
 * cuál de las dos es confirmaría que existe.
 */
public class QuitarActividadExtraServicio implements QuitarActividadExtra {

	private final ActividadExtraRepositorio actividades;

	public QuitarActividadExtraServicio(ActividadExtraRepositorio actividades) {
		this.actividades = actividades;
	}

	@Override
	public void ejecutar(int idEstudiante, int idActividad) {
		if (!actividades.quitar(idEstudiante, idActividad)) {
			throw new ReglaDeNegocioVioladaException("No encontramos esa actividad");
		}
	}
}
