package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Algo de la universidad con día y hora que no es una asignatura: una clase de baile, un semillero, deporte
 * (SCRUM-266). Se ve en Hoy y en Mi horario como una clase más, pero no tiene notas ni cuenta en la Brújula.
 *
 * <p>Es de una sola persona y de un solo día: «lunes y miércoles» son dos actividades. El lugar y el responsable
 * son texto libre y opcionales, porque «Cancha 2» no es un salón con sede ni «Profe Ana» un docente con
 * apellidos.
 *
 * @param id nulo mientras no se haya guardado
 */
public record ActividadExtra(
		Integer id,
		int idEstudiante,
		String nombre,
		DiaDeLaSemana dia,
		LocalTime horaInicio,
		LocalTime horaFin,
		String lugar,
		String responsable) {

	/** Cuántas puede tener cada estudiante: un abuso se corta antes de llenar la base. */
	public static final int MAXIMO_POR_ESTUDIANTE = 20;

	/** Lo que cabe en las columnas nombre, lugar y responsable. */
	static final int LARGO_MAXIMO = 80;

	public ActividadExtra {
		nombre = nombre == null ? "" : nombre.strip();
		if (nombre.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Escribe el nombre de la actividad");
		}
		if (nombre.length() > LARGO_MAXIMO) {
			throw new ReglaDeNegocioVioladaException(
					"El nombre no puede pasar de %d caracteres".formatted(LARGO_MAXIMO));
		}
		if (dia == null) {
			throw new ReglaDeNegocioVioladaException("Falta el día de la actividad");
		}
		if (horaInicio == null || horaFin == null) {
			throw new ReglaDeNegocioVioladaException("Faltan la hora de inicio y la hora de fin de la actividad");
		}
		horaInicio = horaInicio.truncatedTo(ChronoUnit.MINUTES);
		horaFin = horaFin.truncatedTo(ChronoUnit.MINUTES);
		if (!horaInicio.isBefore(horaFin)) {
			throw new ReglaDeNegocioVioladaException(
					"La actividad tiene que empezar antes de terminar: %s no es antes de %s".formatted(horaInicio, horaFin));
		}
		lugar = opcional(lugar, "El lugar");
		responsable = opcional(responsable, "El responsable");
	}

	/** Una actividad recién escrita, todavía sin número. */
	public static ActividadExtra nueva(
			int idEstudiante,
			String nombre,
			DiaDeLaSemana dia,
			LocalTime horaInicio,
			LocalTime horaFin,
			String lugar,
			String responsable) {
		return new ActividadExtra(null, idEstudiante, nombre, dia, horaInicio, horaFin, lugar, responsable);
	}

	/**
	 * La actividad como un bloque del horario, para que se ordene y se compare con las clases. Sin salón ni
	 * docente: el lugar y el responsable viajan en la propia actividad.
	 */
	public BloqueDeHorario comoBloque() {
		return new BloqueDeHorario(1, dia, horaInicio, horaFin, null, null);
	}

	private static String opcional(String texto, String quien) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		String recortado = texto.strip();
		if (recortado.length() > LARGO_MAXIMO) {
			throw new ReglaDeNegocioVioladaException(
					"%s no puede pasar de %d caracteres".formatted(quien, LARGO_MAXIMO));
		}
		return recortado;
	}
}
