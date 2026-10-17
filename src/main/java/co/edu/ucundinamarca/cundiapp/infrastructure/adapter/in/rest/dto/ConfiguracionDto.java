package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion.Configuracion;
import java.math.BigDecimal;

/** Los ajustes del estudiante (RF08, SCRUM-34). Por ahora, su meta de calificación. */
public record ConfiguracionDto(BigDecimal meta) {

	public static ConfiguracionDto desde(Configuracion configuracion) {
		return new ConfiguracionDto(configuracion.meta().valor());
	}
}
