package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion.Configuracion;
import java.math.BigDecimal;

/** Los ajustes del estudiante (RF08, SCRUM-34). Por ahora, su meta de calificación. */
public record ConfiguracionDto(BigDecimal meta, UmbralesDto umbrales) {

	/** Desde qué nota requerida una asignatura pasa a riesgo medio y a riesgo alto (RF09). */
	public record UmbralesDto(BigDecimal medio, BigDecimal alto) {
	}

	public static ConfiguracionDto desde(Configuracion configuracion) {
		return new ConfiguracionDto(configuracion.meta().valor(),
				new UmbralesDto(configuracion.umbrales().medio(), configuracion.umbrales().alto()));
	}
}
