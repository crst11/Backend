package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Un bloque de un grupo que el estudiante escribe al armar su horario de matrícula (RF13, SCRUM-207): un día,
 * de una hora a otra. Es más chico que {@link BloqueDeHorario}, que lleva salón y docente de una clase ya
 * matriculada y aquí no existen.
 *
 * <p>Las horas se guardan al minuto. Dos bloques chocan si son del mismo día y se solapan; terminar a la hora
 * en que empieza el otro no es choque.
 */
public record BloqueDeOferta(DiaDeLaSemana dia, LocalTime inicio, LocalTime fin) {

	public BloqueDeOferta {
		if (dia == null) {
			throw new ReglaDeNegocioVioladaException("Falta el día de la clase");
		}
		if (inicio == null || fin == null) {
			throw new ReglaDeNegocioVioladaException("Faltan la hora de inicio y la hora de fin de la clase");
		}
		inicio = inicio.truncatedTo(ChronoUnit.MINUTES);
		fin = fin.truncatedTo(ChronoUnit.MINUTES);
		if (!inicio.isBefore(fin)) {
			throw new ReglaDeNegocioVioladaException(
					"La clase tiene que empezar antes de terminar: %s no es antes de %s".formatted(inicio, fin));
		}
	}

	public int inicioEnMinutos() {
		return inicio.getHour() * 60 + inicio.getMinute();
	}

	public int finEnMinutos() {
		return fin.getHour() * 60 + fin.getMinute();
	}

	public int minutos() {
		return finEnMinutos() - inicioEnMinutos();
	}

	public boolean chocaCon(BloqueDeOferta otro) {
		return dia == otro.dia && inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
	}
}
