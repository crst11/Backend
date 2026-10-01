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
import java.util.Optional;

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
		Optional<Estudiante> existente = repositorio.buscarPorCorreo(correo);
		if (existente.isPresent() && existente.get().estado() != EstadoCuenta.INACTIVA) {
			throw new CorreoYaRegistradoException("Ya existe una cuenta con ese correo institucional");
		}

		String hash = cifrador.cifrar(datos.contrasenaSinCifrar());
		Estudiante registrado;
		if (existente.isPresent()) {
			// Se había eliminado esta cuenta: se registra de nuevo sobre la misma fila, no como una cuenta aparte.
			Estudiante reactivada = new Estudiante(
					existente.get().id(),
					datos.nombres(),
					datos.apellidos(),
					correo,
					EstadoCuenta.PENDIENTE,
					datos.aceptaTratamientoDatos(),
					reloj.ahora());
			registrado = repositorio.reactivarConCredencialLocal(reactivada, hash);
		} else {
			Estudiante estudiante = new Estudiante(
					null,
					datos.nombres(),
					datos.apellidos(),
					correo,
					EstadoCuenta.PENDIENTE,
					datos.aceptaTratamientoDatos(),
					reloj.ahora());
			registrado = repositorio.guardarConCredencialLocal(estudiante, hash);
		}
		// El correo sale en segundo plano (SCRUM-67): el registro no espera a que el servidor de correo
		// responda. Si el envío falla queda en el log y la persona pide otro código desde la verificación.
		emisor.emitirYEnviar(registrado);
		return registrado;
	}
}
