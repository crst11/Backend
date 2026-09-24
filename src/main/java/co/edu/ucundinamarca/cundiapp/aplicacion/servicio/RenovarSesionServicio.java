package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RenovarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.SesionIniciada;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Sesion;
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
		Sesion nueva = Sesion.abrir(
				estudiante.id(), refrescoNuevo, ahora, vigenciaRefresco,
				IniciarSesionServicio.recortar(origen.userAgent(), 200), IniciarSesionServicio.recortar(origen.ip(), 45));
		sesiones.rotar(usada.revocar(MotivoDeRevocacion.ROTACION, ahora), nueva);

		var acceso = tokens.emitirAcceso(estudiante, ahora, vigenciaAcceso);
		return new SesionIniciada(estudiante, acceso.valor(), acceso.expira(), refrescoNuevo, nueva.fechaExpiracion());
	}
}
