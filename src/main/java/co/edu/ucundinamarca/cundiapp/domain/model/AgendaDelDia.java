package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lo que le toca al estudiante un día (RF07, SCRUM-31 y SCRUM-32): sus clases si ese día hay clase y,
 * junto a ellas, el próximo día que sí tenga, para mostrar mañana o el lunes después de un festivo.
 *
 * <p>**Un festivo o un día fuera del período no muestra clases aunque el horario tenga clase ese día de la
 * semana**: es lo que hace que el lunes festivo no le diga al estudiante que tiene clase. Qué día es lo
 * decide {@link CalendarioAcademico}; aquí solo se cruza con el horario del estudiante.
 *
 * <p>Dentro del día de hoy, cada clase está pasada, en curso, es la próxima o viene después. Eso sale de
 * la hora de ahora, que llega de afuera: así se prueba con cualquier hora sin tocar el reloj.
 */
public record AgendaDelDia(DiaDelCalendario dia, List<ClaseDeLaAgenda> clases, Optional<ProximoDiaConClases> siguiente) {

	/** Cuánto se busca hacia adelante el próximo día con clases: más que un semestre entero. */
	private static final int DIAS_DE_BUSQUEDA = 200;

	public AgendaDelDia {
		clases = List.copyOf(clases);
	}

	/** El día siguiente que sí tiene clases, con ellas. */
	public record ProximoDiaConClases(DiaDelCalendario dia, List<ClaseDelHorario> clases) {

		public ProximoDiaConClases {
			clases = List.copyOf(clases);
		}
	}

	public static AgendaDelDia de(
			LocalDate fecha, LocalDateTime ahora, HorarioSemanal horario, CalendarioAcademico calendario) {
		DiaDelCalendario dia = calendario.clasificar(fecha);
		List<ClaseDelHorario> delDia = dia.tieneClase() ? horario.delDia(dia.diaDeLaSemana()) : List.of();
		return new AgendaDelDia(dia, conEstado(delDia, fecha, ahora), siguienteDespuesDe(fecha, horario, calendario));
	}

	private static List<ClaseDeLaAgenda> conEstado(List<ClaseDelHorario> clases, LocalDate fecha, LocalDateTime ahora) {
		List<ClaseDeLaAgenda> agenda = new ArrayList<>();
		boolean yaHayProxima = false;
		for (ClaseDelHorario clase : clases) {
			EstadoDeLaClase estado;
			if (fecha.isBefore(ahora.toLocalDate())) {
				estado = EstadoDeLaClase.PASADA;
			} else if (fecha.isAfter(ahora.toLocalDate())) {
				estado = EstadoDeLaClase.LUEGO;
			} else {
				LocalTime hora = ahora.toLocalTime();
				if (!hora.isBefore(clase.bloque().horaFin())) {
					estado = EstadoDeLaClase.PASADA;
				} else if (!hora.isBefore(clase.bloque().horaInicio())) {
					estado = EstadoDeLaClase.EN_CURSO;
				} else if (!yaHayProxima) {
					estado = EstadoDeLaClase.PROXIMA;
					yaHayProxima = true;
				} else {
					estado = EstadoDeLaClase.LUEGO;
				}
			}
			agenda.add(new ClaseDeLaAgenda(clase, estado));
		}
		return agenda;
	}

	private static Optional<ProximoDiaConClases> siguienteDespuesDe(
			LocalDate fecha, HorarioSemanal horario, CalendarioAcademico calendario) {
		if (horario.estaVacio()) {
			return Optional.empty();
		}
		for (int i = 1; i <= DIAS_DE_BUSQUEDA; i++) {
			DiaDelCalendario candidato = calendario.clasificar(fecha.plusDays(i));
			if (!candidato.tieneClase()) {
				continue;
			}
			List<ClaseDelHorario> clases = horario.delDia(candidato.diaDeLaSemana());
			if (!clases.isEmpty()) {
				return Optional.of(new ProximoDiaConClases(candidato, clases));
			}
		}
		return Optional.empty();
	}
}
