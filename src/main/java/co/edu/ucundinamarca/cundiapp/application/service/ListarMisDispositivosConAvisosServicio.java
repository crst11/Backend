package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisDispositivosConAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.out.DispositivoConAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import java.util.List;

/** Los dispositivos que hoy pueden recibir mis avisos (RF10, SCRUM-38). */
public class ListarMisDispositivosConAvisosServicio implements ListarMisDispositivosConAvisos {

	private final DispositivoConAvisosRepositorio dispositivos;

	public ListarMisDispositivosConAvisosServicio(DispositivoConAvisosRepositorio dispositivos) {
		this.dispositivos = dispositivos;
	}

	@Override
	public List<DispositivoConAvisos> ejecutar(int idEstudiante) {
		return dispositivos.activos(idEstudiante);
	}
}
