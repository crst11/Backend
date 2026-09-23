package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.DatosDeRegistro;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

public class RegistrarEstudianteServicio implements RegistrarEstudiante {

	private final EstudianteRepositorio repositorio;
	private final CifradorDeContrasenaPort cifrador;
	private final RelojPort reloj;

	public RegistrarEstudianteServicio(
			EstudianteRepositorio repositorio, CifradorDeContrasenaPort cifrador, RelojPort reloj) {
		this.repositorio = repositorio;
		this.cifrador = cifrador;
		this.reloj = reloj;
	}

	@Override
	public Estudiante ejecutar(DatosDeRegistro datos) {
		CorreoInstitucional correo = new CorreoInstitucional(datos.correo());
		if (repositorio.existeCuentaCon(correo)) {
			throw new CorreoYaRegistradoException("Ya existe una cuenta con ese correo institucional");
		}

		Estudiante estudiante = new Estudiante(
				null,
				datos.nombres(),
				datos.apellidos(),
				correo,
				EstadoCuenta.PENDIENTE,
				datos.aceptaTratamientoDatos(),
				reloj.ahora());

		String hash = cifrador.cifrar(datos.contrasenaSinCifrar());
		return repositorio.guardarConCredencialLocal(estudiante, hash);
	}
}
