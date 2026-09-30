package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RenovarSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.SesionIniciada;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Duration;
import java.time.Instant;

public class RenovarSesionServicio implements RenovarSesion {

	private final EstudianteRepositorio estudiantes;
	private final SesionRepositorio sesiones;
	private final EmisorDeTokensPort tokens;
	private final RelojPort reloj;
	private final Duration vigenciaAcceso;
	private final Duration vigenciaRefresco;

	public RenovarSesionServicio(
			EstudianteRepositorio estudiantes,
			SesionRepositorio sesiones,
			EmisorDeTokensPort tokens,
			RelojPort reloj,
			Duration vigenciaAcceso,
			Duration vigenciaRefresco) {
		this.estudiantes = estudiantes;
		this.sesiones = sesiones;
		this.tokens = tokens;
		this.reloj = reloj;
		this.vigenciaAcceso = vigenciaAcceso;
		this.vigenciaRefresco = vigenciaRefresco;
	}

	@Override
	public SesionIniciada ejecutar(String tokenDeRefresco, OrigenDeSesion origen) {
		Instant ahora = reloj.ahora();
		Sesion usada = sesiones.buscarPorHuella(Sesion.huellaDe(tokenDeRefresco))
				.orElseThrow(SesionInvalidaException::new);

		if (usada.fueRotada()) {
			// Ese refresco ya se había cambiado por otro: alguien lo copió. Se cierran todas las sesiones de la cuenta.
			sesiones.revocarVigentes(usada.idEstudiante(), MotivoDeRevocacion.REUSO_DETECTADO, ahora);
			throw new SesionInvalidaException();
		}
		if (!usada.estaVigente(ahora)) {
			throw new SesionInvalidaException();
		}
		Estudiante estudiante = estudiantes.buscarPorId(usada.idEstudiante())
				.filter(e -> e.estado() == EstadoCuenta.ACTIVA)
				.orElseThrow(SesionInvalidaException::new);

		String refrescoNuevo = tokens.generarTokenDeRefresco();
		// La sesión que nace de la rotación conserva el método con que se entró (contraseña o Google).
		Sesion nueva = Sesion.abrir(
				estudiante.id(), usada.metodo(), refrescoNuevo, ahora, vigenciaRefresco,
				AbridorDeSesion.recortar(origen.userAgent(), 200), AbridorDeSesion.recortar(origen.ip(), 45));
		sesiones.rotar(usada.revocar(MotivoDeRevocacion.ROTACION, ahora), nueva);

		var acceso = tokens.emitirAcceso(estudiante, ahora, vigenciaAcceso);
		return new SesionIniciada(estudiante, acceso.valor(), acceso.expira(), refrescoNuevo, nueva.fechaExpiracion());
	}
}
