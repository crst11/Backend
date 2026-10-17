package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiSuscripcionPush;
import co.edu.ucundinamarca.cundiapp.application.port.out.DispositivoConAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.util.List;

/** Guarda la suscripción Web Push del dispositivo desde el que la persona activó los avisos (RF10, SCRUM-37). */
public class RegistrarMiSuscripcionPushServicio implements RegistrarMiSuscripcionPush {

	static final String NOMBRE_POR_DEFECTO = "Este dispositivo";
	private static final int LARGO_MAXIMO_DEL_NOMBRE = 80;
	static final int MAXIMO_DE_DISPOSITIVOS = 10;

	private final DispositivoConAvisosRepositorio dispositivos;
	private final RelojPort reloj;

	public RegistrarMiSuscripcionPushServicio(DispositivoConAvisosRepositorio dispositivos, RelojPort reloj) {
		this.dispositivos = dispositivos;
		this.reloj = reloj;
	}

	@Override
	public DispositivoConAvisos ejecutar(int idEstudiante, String nombre, SuscripcionPush suscripcion) {
		if (suscripcion == null) {
			throw new ReglaDeNegocioVioladaException("La suscripción a los avisos no es válida");
		}
		validarElTope(idEstudiante, suscripcion);
		return dispositivos.guardarSuscripcion(idEstudiante, nombreLimpio(nombre), suscripcion, reloj.ahora());
	}

	/**
	 * Cada aviso se manda a todos los dispositivos de la persona: sin tope, alguien podría registrar cientos de
	 * suscripciones y hacer que cada aviso suyo se multiplique. Volver a suscribir el mismo navegador no cuenta.
	 */
	private void validarElTope(int idEstudiante, SuscripcionPush suscripcion) {
		List<DispositivoConAvisos> activos = dispositivos.activos(idEstudiante);
		boolean yaEstaba = activos.stream().anyMatch(d -> d.suscripcion().endpoint().equals(suscripcion.endpoint()));
		if (!yaEstaba && activos.size() >= MAXIMO_DE_DISPOSITIVOS) {
			throw new ReglaDeNegocioVioladaException(
					"Ya tienes " + MAXIMO_DE_DISPOSITIVOS + " dispositivos con avisos. Quita uno para activar este");
		}
	}

	private static String nombreLimpio(String nombre) {
		String limpio = nombre == null ? "" : nombre.trim();
		if (limpio.isEmpty()) {
			return NOMBRE_POR_DEFECTO;
		}
		return limpio.length() > LARGO_MAXIMO_DEL_NOMBRE ? limpio.substring(0, LARGO_MAXIMO_DEL_NOMBRE) : limpio;
	}
}
