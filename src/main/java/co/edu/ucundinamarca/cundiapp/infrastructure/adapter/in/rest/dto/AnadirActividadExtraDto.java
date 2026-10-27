package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnadirActividadExtra.DatosDeLaActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

/**
 * Una actividad extra nueva, como la manda la pantalla (SCRUM-266).
 *
 * <p>Solo se exige que vengan los datos: que el nombre no esté vacío, que las horas tengan sentido y que el día
 * exista lo comprueba el dominio, para que el mensaje al estudiante sea el mismo venga de donde venga. Los siete
 * días de la semana son el máximo de {@code dias}: más es un abuso, no un descuido.
 */
public record AnadirActividadExtraDto(
		String nombre,
		@NotNull @Size(max = 7) List<String> dias,
		@NotNull LocalTime horaInicio,
		@NotNull LocalTime horaFin,
		String lugar,
		String responsable) {

	public DatosDeLaActividad aDatos() {
		return new DatosDeLaActividad(
				nombre, dias.stream().map(DiaDeLaSemana::desdeBd).toList(), horaInicio, horaFin, lugar, responsable);
	}
}
