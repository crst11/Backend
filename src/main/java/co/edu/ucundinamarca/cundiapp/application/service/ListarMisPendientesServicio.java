package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisPendientes;
import co.edu.ucundinamarca.cundiapp.application.port.out.PendienteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.PendienteConSituacion;
import java.time.Clock;
import java.util.List;

/** Mis pendientes ordenados y con su situación (RF06, SCRUM-30): qué es urgente lo decide el dominio. */
public class ListarMisPendientesServicio implements ListarMisPendientes {

	private final PendienteRepositorio pendientes;
	private final Clock reloj;

	public ListarMisPendientesServicio(PendienteRepositorio pendientes, Clock reloj) {
		this.pendientes = pendientes;
		this.reloj = reloj;
	}

	@Override
	public List<PendienteConSituacion> ejecutar(int idEstudiante, boolean incluirCumplidos) {
		return PendienteConSituacion.ordenar(pendientes.listar(idEstudiante, incluirCumplidos), reloj.instant());
	}
}
