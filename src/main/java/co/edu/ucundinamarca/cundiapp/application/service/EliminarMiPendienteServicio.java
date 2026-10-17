package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.EliminarMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.out.PendienteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/** Borra un pendiente (RF06, SCRUM-29). */
public class EliminarMiPendienteServicio implements EliminarMiPendiente {

	private final PendienteRepositorio pendientes;

	public EliminarMiPendienteServicio(PendienteRepositorio pendientes) {
		this.pendientes = pendientes;
	}

	@Override
	public void ejecutar(int idEstudiante, int idPendiente) {
		if (!pendientes.eliminar(idEstudiante, idPendiente)) {
			throw new ReglaDeNegocioVioladaException("No encontramos ese pendiente");
		}
	}
}
