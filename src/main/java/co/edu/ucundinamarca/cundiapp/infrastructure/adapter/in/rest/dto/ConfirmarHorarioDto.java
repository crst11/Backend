package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario.Confirmacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.AgregarClaseAlHorario.DatosDeLaClase;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que el estudiante confirmó del Reporte de Horario (SCRUM-81). Solo se exige lo básico; que las
 * horas tengan sentido, que el día exista y que cada salón venga con su sede lo comprueba el
 * dominio, para que el mensaje sea el mismo venga de donde venga.
 */
public record ConfirmarHorarioDto(
		@NotBlank String periodo,
		@NotEmpty @Valid List<AsignaturaDto> asignaturas) {

	public record AsignaturaDto(@NotBlank String codigo, String grupo, @Valid List<AgregarClaseDto> clases) {
	}

	public Confirmacion aConfirmacion() {
		return new Confirmacion(periodo, asignaturas.stream().map(ConfirmarHorarioDto::aAsignatura).toList());
	}

	private static AsignaturaConHorario aAsignatura(AsignaturaDto asignatura) {
		List<BloqueDeHorario> bloques = new ArrayList<>();
		List<AgregarClaseDto> clases = asignatura.clases() == null ? List.of() : asignatura.clases();
		for (int i = 0; i < clases.size(); i++) {
			DatosDeLaClase datos = clases.get(i).aDatos();
			bloques.add(new BloqueDeHorario(
					i + 1, datos.dia(), datos.horaInicio(), datos.horaFin(), datos.espacio(), datos.docente()));
		}
		return new AsignaturaConHorario(asignatura.codigo(), asignatura.grupo(), bloques);
	}
}
