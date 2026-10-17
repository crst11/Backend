package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;

/** Mis avisos, los más recientes primero (RF10, SCRUM-37). */
public class ListarMisAvisosServicio implements ListarMisAvisos {

	/** Los más recientes bastan para la campana; el resto de la historia no hace falta en pantalla. */
	private static final int MAXIMO_DE_AVISOS = 50;

	private final AvisoRepositorio avisos;

	public ListarMisAvisosServicio(AvisoRepositorio avisos) {
		this.avisos = avisos;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, boolean soloSinLeer) {
		return new Resultado(avisos.sinLeer(idEstudiante), avisos.deEstudiante(idEstudiante, soloSinLeer, MAXIMO_DE_AVISOS));
	}
}
