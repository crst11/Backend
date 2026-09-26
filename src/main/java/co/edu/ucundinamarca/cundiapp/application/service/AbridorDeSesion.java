package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.application.port.in.SesionIniciada;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Duration;
import java.time.Instant;

/**
 * Abre la sesión de una cuenta ya autenticada, venga de la contraseña o de Google: guarda la huella
 * del refresco y emite el token de acceso. Quien la llama ya comprobó quién es y que la cuenta está activa.
 */
public class AbridorDeSesion {

	private final SesionRepositorio sesiones;
	private final EmisorDeTokensPort tokens;
	private final Duration vigenciaAcceso;
	private final Duration vigenciaRefresco;

	public AbridorDeSesion(
			SesionRepositorio sesiones, EmisorDeTokensPort tokens, Duration vigenciaAcceso, Duration vigenciaRefresco) {
		this.sesiones = sesiones;
		this.tokens = tokens;
		this.vigenciaAcceso = vigenciaAcceso;
		this.vigenciaRefresco = vigenciaRefresco;
	}

	public SesionIniciada abrir(Estudiante cuenta, MetodoDeAcceso metodo, OrigenDeSesion origen, Instant ahora) {
		String refresco = tokens.generarTokenDeRefresco();
		Sesion sesion = Sesion.abrir(
				cuenta.id(), metodo, refresco, ahora, vigenciaRefresco,
				recortar(origen.userAgent(), 200), recortar(origen.ip(), 45));
		sesiones.guardar(sesion);

		var acceso = tokens.emitirAcceso(cuenta, ahora, vigenciaAcceso);
		return new SesionIniciada(cuenta, acceso.valor(), acceso.expira(), refresco, sesion.fechaExpiracion());
	}

	static String recortar(String texto, int maximo) {
		if (texto == null) {
			return null;
		}
		return texto.length() <= maximo ? texto : texto.substring(0, maximo);
	}
}
