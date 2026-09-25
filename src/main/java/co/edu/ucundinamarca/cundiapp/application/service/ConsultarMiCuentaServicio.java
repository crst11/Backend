package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiCuenta;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

public class ConsultarMiCuentaServicio implements ConsultarMiCuenta {

	private final EstudianteRepositorio estudiantes;

	public ConsultarMiCuentaServicio(EstudianteRepositorio estudiantes) {
		this.estudiantes = estudiantes;
	}

	@Override
	public Estudiante ejecutar(int idEstudiante) {
		return estudiantes.buscarPorId(idEstudiante).orElseThrow(SesionInvalidaException::new);
	}
}
