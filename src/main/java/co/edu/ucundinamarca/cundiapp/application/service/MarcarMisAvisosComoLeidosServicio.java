package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.MarcarMisAvisosComoLeidos;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;

/** Marca todos mis avisos como leídos (RF10, SCRUM-37). */
public class MarcarMisAvisosComoLeidosServicio implements MarcarMisAvisosComoLeidos {

	private final AvisoRepositorio avisos;

	public MarcarMisAvisosComoLeidosServicio(AvisoRepositorio avisos) {
		this.avisos = avisos;
	}

	@Override
	public void ejecutar(int idEstudiante) {
		avisos.marcarTodosLeidos(idEstudiante);
	}
}
