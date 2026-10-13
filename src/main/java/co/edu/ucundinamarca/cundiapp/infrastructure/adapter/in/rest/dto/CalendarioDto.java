package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarCalendario.Calendario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDelCalendario;
import java.util.List;

/**
 * El calendario como lo lee la pantalla (SCRUM-85): la fecha de hoy en Bogotá y un día por fecha. `dia`
 * viaja sin tildes (`lunes`, `miercoles`) como en el horario, y `festivo` solo viene en los festivos.
 */
public record CalendarioDto(String hoy, List<DiaDto> dias) {

	public record DiaDto(String fecha, String dia, String tipo, String festivo) {

		static DiaDto desde(DiaDelCalendario dia) {
			return new DiaDto(
					dia.fecha().toString(), dia.diaDeLaSemana().valorEnBd(), dia.tipo().valor(), dia.nombreDelFestivo());
		}
	}

	public static CalendarioDto desde(Calendario calendario) {
		return new CalendarioDto(
				calendario.hoy().toString(), calendario.dias().stream().map(DiaDto::desde).toList());
	}
}
