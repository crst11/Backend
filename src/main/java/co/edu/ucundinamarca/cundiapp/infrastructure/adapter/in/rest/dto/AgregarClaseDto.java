package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.AgregarClaseAlHorario.DatosDeLaClase;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.Docente;
import co.edu.ucundinamarca.cundiapp.domain.model.EspacioFisico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * Un bloque nuevo del horario, como lo manda la pantalla (SCRUM-25).
 *
 * <p>Solo se exige lo básico: el día y las horas. Que las horas tengan sentido, que el día exista y
 * que el salón venga con su sede lo comprueba el dominio, para que el mensaje al estudiante sea el
 * mismo venga de donde venga. Si no se escribe nada de lugar, el bloque queda sin salón.
 */
public record AgregarClaseDto(
		@NotBlank String dia,
		@NotNull LocalTime horaInicio,
		@NotNull LocalTime horaFin,
		String salon,
		String bloque,
		String sede,
		@Valid DocenteDto docente) {

	public record DocenteDto(String nombres, String apellidos) {
	}

	public DatosDeLaClase aDatos() {
		boolean hayLugar = hayTexto(salon) || hayTexto(bloque) || hayTexto(sede);
		boolean hayDocente = docente != null && (hayTexto(docente.nombres()) || hayTexto(docente.apellidos()));
		return new DatosDeLaClase(
				DiaDeLaSemana.desdeBd(dia),
				horaInicio,
				horaFin,
				hayLugar ? new EspacioFisico(salon, bloque, sede) : null,
				hayDocente ? new Docente(docente.nombres(), docente.apellidos()) : null);
	}

	private static boolean hayTexto(String texto) {
		return texto != null && !texto.isBlank();
	}
}
