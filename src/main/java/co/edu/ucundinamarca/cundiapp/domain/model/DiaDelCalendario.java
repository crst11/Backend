package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.LocalDate;
import java.util.Optional;

/** Una fecha con la clase de día que es y, si es festivo, cuál (RF07, SCRUM-85). */
public record DiaDelCalendario(LocalDate fecha, TipoDeDia tipo, String nombreDelFestivo) {

	public Optional<String> festivo() {
		return Optional.ofNullable(nombreDelFestivo);
	}

	/**
	 * Solo en un día lectivo puede haber clase. Que de verdad la haya depende además del horario de cada
	 * estudiante: un martes lectivo sin clases en su horario es un día libre para él.
	 */
	public boolean tieneClase() {
		return tipo == TipoDeDia.LECTIVO;
	}

	public DiaDeLaSemana diaDeLaSemana() {
		return DiaDeLaSemana.de(fecha.getDayOfWeek());
	}
}
