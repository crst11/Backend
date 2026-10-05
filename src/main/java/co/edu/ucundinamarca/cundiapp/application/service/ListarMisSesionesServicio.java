package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisSesiones;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConSesion;
import java.util.List;

public class ListarMisSesionesServicio implements ListarMisSesiones {

	private final SesionRepositorio sesiones;
	private final RelojPort reloj;

	public ListarMisSesionesServicio(SesionRepositorio sesiones, RelojPort reloj) {
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	@Override
	public List<DispositivoConSesion> ejecutar(int idEstudiante, Integer sesionActual) {
		return DispositivoConSesion.agrupar(sesiones.listarVigentes(idEstudiante, reloj.ahora()), sesionActual);
	}
}
