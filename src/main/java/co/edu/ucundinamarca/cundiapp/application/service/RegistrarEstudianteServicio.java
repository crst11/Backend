package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.DatosDeRegistro;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

public class RegistrarEstudianteServicio implements RegistrarEstudiante {

	private final EstudianteRepositorio repositorio;
	private final CifradorDeContrasenaPort cifrador;
	private final RelojPort reloj;
	private final EmisorDeCodigoDeVerificacion emisor;

	public RegistrarEstudianteServicio(
			EstudianteRepositorio repositorio,
			CifradorDeContrasenaPort cifrador,
			RelojPort reloj,
			EmisorDeCodigoDeVerificacion emisor) {
		this.repositorio = repositorio;
		this.cifrador = cifrador;
		this.reloj = reloj;
		this.emisor = emisor;
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
		Estudiante registrado = repositorio.guardarConCredencialLocal(estudiante, hash);
		emisor.emitirYEnviar(registrado);
		return registrado;
	}
}
