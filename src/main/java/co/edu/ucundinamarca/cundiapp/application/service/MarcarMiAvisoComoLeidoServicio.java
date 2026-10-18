package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.MarcarMiAvisoComoLeido;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/** Marca un aviso mío como leído; uno ajeno o inexistente responde igual (RF10, SCRUM-37). */
public class MarcarMiAvisoComoLeidoServicio implements MarcarMiAvisoComoLeido {

	private final AvisoRepositorio avisos;

	public MarcarMiAvisoComoLeidoServicio(AvisoRepositorio avisos) {
		this.avisos = avisos;
	}

	@Override
	public void ejecutar(int idEstudiante, int idAviso, boolean leido) {
		if (!avisos.marcarLeido(idEstudiante, idAviso, leido)) {
			throw new ReglaDeNegocioVioladaException("No encontramos ese aviso");
		}
	}
}
