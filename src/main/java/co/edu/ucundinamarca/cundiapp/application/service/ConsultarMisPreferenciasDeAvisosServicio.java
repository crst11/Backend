package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMisPreferenciasDeAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciasDeAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;

/** Las preferencias de avisos tal como están guardadas (RF10, SCRUM-38). */
public class ConsultarMisPreferenciasDeAvisosServicio implements ConsultarMisPreferenciasDeAvisos {

	private final PreferenciasDeAvisosRepositorio preferencias;

	public ConsultarMisPreferenciasDeAvisosServicio(PreferenciasDeAvisosRepositorio preferencias) {
		this.preferencias = preferencias;
	}

	@Override
	public PreferenciasDeAvisos ejecutar(int idEstudiante) {
		return preferencias.de(idEstudiante);
	}
}
