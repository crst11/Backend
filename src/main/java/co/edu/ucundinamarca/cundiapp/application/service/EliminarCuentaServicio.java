package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.EliminarCuenta;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;

public class EliminarCuentaServicio implements EliminarCuenta {

	private final EstudianteRepositorio estudiantes;
	private final SesionRepositorio sesiones;
	private final RelojPort reloj;

	public EliminarCuentaServicio(EstudianteRepositorio estudiantes, SesionRepositorio sesiones, RelojPort reloj) {
		this.estudiantes = estudiantes;
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	@Override
	public void ejecutar(int idEstudiante) {
		Estudiante cuenta = estudiantes.buscarPorId(idEstudiante).orElseThrow(SesionInvalidaException::new);
		estudiantes.guardarDesactivacion(cuenta.desactivar());
		sesiones.revocarVigentes(idEstudiante, MotivoDeRevocacion.CIERRE_SESION, reloj.ahora());
	}
}
