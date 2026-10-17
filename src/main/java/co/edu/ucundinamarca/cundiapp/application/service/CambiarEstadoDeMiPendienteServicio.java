package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarEstadoDeMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.out.PendienteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Pendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.PendienteConSituacion;
import java.time.Clock;

/** Marca un pendiente como hecho o lo reabre (RF06, SCRUM-29). */
public class CambiarEstadoDeMiPendienteServicio implements CambiarEstadoDeMiPendiente {

	private final PendienteRepositorio pendientes;
	private final Clock reloj;

	public CambiarEstadoDeMiPendienteServicio(PendienteRepositorio pendientes, Clock reloj) {
		this.pendientes = pendientes;
		this.reloj = reloj;
	}

	@Override
	public PendienteConSituacion ejecutar(int idEstudiante, int idPendiente, boolean cumplido) {
		Pendiente actual = pendientes.buscar(idEstudiante, idPendiente)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException("No encontramos ese pendiente"));
		Pendiente cambiado = cumplido ? actual.cumplir() : actual.reabrir();
		return PendienteConSituacion.de(pendientes.guardar(cambiado), reloj.instant());
	}
}
