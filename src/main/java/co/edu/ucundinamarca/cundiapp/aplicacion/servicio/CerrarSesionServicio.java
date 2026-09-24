package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.CerrarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Sesion;

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
