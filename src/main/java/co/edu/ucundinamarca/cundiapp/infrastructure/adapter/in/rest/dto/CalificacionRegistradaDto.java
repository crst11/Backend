package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiCalificacion;
import java.math.BigDecimal;

/** La nota que quedó guardada y lo que el estudiante lleva ya con ella (SCRUM-28). */
public record CalificacionRegistradaDto(BigDecimal nota, EstructuraDeEvaluacionDto.AcumuladoDto acumulado) {

	public static CalificacionRegistradaDto desde(RegistrarMiCalificacion.Resultado resultado) {
		return new CalificacionRegistradaDto(
				resultado.nota().valor(), EstructuraDeEvaluacionDto.AcumuladoDto.desde(resultado.acumulado()));
	}
}
