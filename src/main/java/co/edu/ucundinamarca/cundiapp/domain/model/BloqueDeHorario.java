package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Un bloque de una clase: un día, de una hora a otra, en un lugar y con un docente (RF04, SCRUM-25).
 *
 * <p>Una asignatura suele tener varios: lunes de 8 a 10 y miércoles de 8 a 10 son dos bloques. El
 * lugar y el docente son opcionales, porque el estudiante puede conocer el horario antes de saber
 * el salón.
 *
 * <p>Las horas se guardan al minuto: los segundos no significan nada en un horario de clases.
 */
public record BloqueDeHorario(
		int consecutivo,
		DiaDeLaSemana dia,
		LocalTime horaInicio,
		LocalTime horaFin,
		EspacioFisico espacioFisico,
		Docente docenteAsignado) {

	public BloqueDeHorario {
		if (consecutivo <= 0) {
			throw new ReglaDeNegocioVioladaException("El número de orden del bloque empieza en 1");
		}
		if (dia == null) {
			throw new ReglaDeNegocioVioladaException("Falta el día de la clase");
		}
		if (horaInicio == null || horaFin == null) {
			throw new ReglaDeNegocioVioladaException("Faltan la hora de inicio y la hora de fin de la clase");
		}
		horaInicio = horaInicio.truncatedTo(ChronoUnit.MINUTES);
		horaFin = horaFin.truncatedTo(ChronoUnit.MINUTES);
		if (!horaInicio.isBefore(horaFin)) {
			throw new ReglaDeNegocioVioladaException(
					"La clase tiene que empezar antes de terminar: %s no es antes de %s".formatted(horaInicio, horaFin));
		}
	}

	public Optional<EspacioFisico> espacio() {
		return Optional.ofNullable(espacioFisico);
	}

	public Optional<Docente> docente() {
		return Optional.ofNullable(docenteAsignado);
	}

	public long duracionEnMinutos() {
		return Duration.between(horaInicio, horaFin).toMinutes();
	}
}
