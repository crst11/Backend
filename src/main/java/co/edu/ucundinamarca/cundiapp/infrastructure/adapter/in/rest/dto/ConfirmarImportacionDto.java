package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.Confirmacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.NotaConfirmada;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.PeriodoConfirmado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

/** Lo que el estudiante confirmó, con las correcciones que haya hecho (SCRUM-23). */
public record ConfirmarImportacionDto(
		@NotBlank String nombreArchivo,
		int detectadas,
		@NotEmpty @Valid List<PeriodoDto> periodos) {

	public record PeriodoDto(
			@NotBlank String codigo,
			int creditosMatriculados,
			int creditosAprobados,
			BigDecimal promedioPeriodo,
			BigDecimal promedioAcumulado,
			@Valid List<NotaDto> notas) {
	}

	public record NotaDto(@NotBlank String codigoAsignatura, BigDecimal nota) {
	}

	public Confirmacion aConfirmacion() {
		return new Confirmacion(nombreArchivo, detectadas, periodos.stream()
				.map(periodo -> new PeriodoConfirmado(
						periodo.codigo(),
						periodo.creditosMatriculados(),
						periodo.creditosAprobados(),
						periodo.promedioPeriodo(),
						periodo.promedioAcumulado(),
						periodo.notas() == null ? List.of() : periodo.notas().stream()
								.map(nota -> new NotaConfirmada(nota.codigoAsignatura(), nota.nota()))
								.toList()))
				.toList());
	}
}
