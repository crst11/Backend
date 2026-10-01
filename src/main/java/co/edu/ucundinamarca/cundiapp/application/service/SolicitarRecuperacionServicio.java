package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.SolicitarRecuperacionDeContrasena;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;

/**
 * Envía un código para cambiar la contraseña (SCRUM-68).
 *
 * <p>Pase lo que pase, termina sin decir nada: si respondiera distinto cuando la cuenta no existe,
 * cualquiera podría averiguar qué correos están registrados probando uno por uno.
 */
public class SolicitarRecuperacionServicio implements SolicitarRecuperacionDeContrasena {

	private final EstudianteRepositorio estudiantes;
	private final CodigoDeVerificacionRepositorio codigos;
	private final EmisorDeCodigoDeVerificacion emisor;
	private final RelojPort reloj;

	public SolicitarRecuperacionServicio(
			EstudianteRepositorio estudiantes,
			CodigoDeVerificacionRepositorio codigos,
			EmisorDeCodigoDeVerificacion emisor,
			RelojPort reloj) {
		this.estudiantes = estudiantes;
		this.codigos = codigos;
		this.emisor = emisor;
		this.reloj = reloj;
	}

	@Override
	public void ejecutar(String correo) {
		estudiantes.buscarPorCorreo(new CorreoInstitucional(correo))
				.filter(Estudiante::estaActiva)
				// Sin contraseña propia no hay nada que recuperar: esa cuenta solo entra con Google.
				.filter(estudiante -> estudiantes.contrasenaCifradaDe(estudiante.id()).isPresent())
				.filter(this::noSeAcabaDePedirUno)
				.ifPresent(estudiante -> emisor.emitirYEnviar(estudiante, PropositoDelCodigo.RECUPERAR_CONTRASENA));
	}

	/** Evita que pedirlo varias veces seguidas llene de correos la bandeja de alguien más. */
	private boolean noSeAcabaDePedirUno(Estudiante estudiante) {
		return codigos.buscarDe(estudiante.id(), PropositoDelCodigo.RECUPERAR_CONTRASENA)
				.map(codigo -> codigo.puedeReemitirse(reloj.ahora()))
				.orElse(true);
	}
}
