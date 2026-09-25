package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CerrarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;

public class CerrarSesionServicio implements CerrarSesion {

	private final SesionRepositorio sesiones;
	private final RelojPort reloj;

	public CerrarSesionServicio(SesionRepositorio sesiones, RelojPort reloj) {
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	@Override
	public void ejecutar(String tokenDeRefresco) {
		sesiones.buscarPorHuella(Sesion.huellaDe(tokenDeRefresco))
				.filter(sesion -> !sesion.estaRevocada())
				.ifPresent(sesion -> sesiones.actualizar(sesion.revocar(MotivoDeRevocacion.CIERRE_SESION, reloj.ahora())));
	}
}
