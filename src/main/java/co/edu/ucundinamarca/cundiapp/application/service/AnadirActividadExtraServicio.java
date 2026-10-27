package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnadirActividadExtra;
import co.edu.ucundinamarca.cundiapp.application.port.out.ActividadExtraRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadExtra;
import java.util.List;

/**
 * Añade una actividad extra (SCRUM-266). Qué es válido lo decide {@link ActividadExtra}; aquí se reparte en una
 * fila por día y se deja que el repositorio cuente y guarde juntos, para que dos peticiones a la vez no pasen
 * del límite.
 */
public class AnadirActividadExtraServicio implements AnadirActividadExtra {

	private final ActividadExtraRepositorio actividades;

	public AnadirActividadExtraServicio(ActividadExtraRepositorio actividades) {
		this.actividades = actividades;
	}

	@Override
	public List<ActividadExtra> ejecutar(int idEstudiante, DatosDeLaActividad datos) {
		if (datos.dias() == null || datos.dias().isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Elige al menos un día");
		}
		List<ActividadExtra> porDia = datos.dias().stream()
				.distinct()
				.map(dia -> ActividadExtra.nueva(
						idEstudiante, datos.nombre(), dia, datos.horaInicio(), datos.horaFin(), datos.lugar(),
						datos.responsable()))
				.toList();
		return actividades.guardarSiCaben(idEstudiante, porDia, ActividadExtra.MAXIMO_POR_ESTUDIANTE)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Puedes tener hasta " + ActividadExtra.MAXIMO_POR_ESTUDIANTE + " actividades extra"));
	}
}
