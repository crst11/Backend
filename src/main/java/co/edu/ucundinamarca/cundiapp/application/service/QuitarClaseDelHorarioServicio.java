package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Quita un bloque del horario (RF04, SCRUM-25). Quitar uno que ya no está se rechaza, igual que
 * cerrar una sesión ya cerrada: es señal de que la pantalla quedó desactualizada.
 */
public class QuitarClaseDelHorarioServicio implements QuitarClaseDelHorario {

	private final HorarioRepositorio horarios;

	public QuitarClaseDelHorarioServicio(HorarioRepositorio horarios) {
		this.horarios = horarios;
	}

	@Override
	public void ejecutar(int idEstudiante, int idMatricula, int consecutivoDelBloque) {
		if (!horarios.quitar(idEstudiante, idMatricula, consecutivoDelBloque)) {
			throw new ReglaDeNegocioVioladaException("No encontramos esa clase en tu horario");
		}
	}
}
