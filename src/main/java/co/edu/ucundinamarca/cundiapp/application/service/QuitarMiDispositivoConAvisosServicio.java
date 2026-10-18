package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarMiDispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.out.DispositivoConAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/** Quita uno de mis dispositivos de los avisos; uno ajeno o inexistente responde igual (RF10, SCRUM-38). */
public class QuitarMiDispositivoConAvisosServicio implements QuitarMiDispositivoConAvisos {

	private final DispositivoConAvisosRepositorio dispositivos;

	public QuitarMiDispositivoConAvisosServicio(DispositivoConAvisosRepositorio dispositivos) {
		this.dispositivos = dispositivos;
	}

	@Override
	public void ejecutar(int idEstudiante, int consecutivo) {
		if (!dispositivos.quitar(idEstudiante, consecutivo)) {
			throw new ReglaDeNegocioVioladaException("No encontramos ese dispositivo");
		}
	}
}
