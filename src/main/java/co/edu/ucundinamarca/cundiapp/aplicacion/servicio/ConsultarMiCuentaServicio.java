package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ConsultarMiCuenta;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

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
