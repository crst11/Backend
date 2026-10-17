package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMisPreferenciasDeAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciasDeAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;

/** Guarda las preferencias de avisos. Que sean válidas lo garantiza {@link PreferenciasDeAvisos} (RF10, SCRUM-38). */
public class CambiarMisPreferenciasDeAvisosServicio implements CambiarMisPreferenciasDeAvisos {

	private final PreferenciasDeAvisosRepositorio preferencias;

	public CambiarMisPreferenciasDeAvisosServicio(PreferenciasDeAvisosRepositorio preferencias) {
		this.preferencias = preferencias;
	}

	@Override
	public PreferenciasDeAvisos ejecutar(int idEstudiante, PreferenciasDeAvisos nuevas) {
		if (nuevas == null) {
			throw new ReglaDeNegocioVioladaException("Faltan las preferencias de avisos");
		}
		preferencias.guardar(idEstudiante, nuevas);
		return nuevas;
	}
}
