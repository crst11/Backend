package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;
import jakarta.validation.constraints.NotNull;

/**
 * Las preferencias de avisos, en las dos direcciones (SCRUM-38). Todos los campos son obligatorios al
 * guardar: con booleanos simples, un campo que falte se leería como «apagado» y desactivaría un aviso sin que
 * la persona lo pidiera. El rango de la anticipación lo comprueba el dominio.
 */
public record PreferenciasDeAvisosDto(
		@NotNull Boolean activos,
		@NotNull Boolean vencimientos,
		@NotNull Boolean cambioDeRiesgo,
		@NotNull Integer anticipacionEnHoras) {

	public PreferenciasDeAvisos aPreferencias() {
		return new PreferenciasDeAvisos(activos, vencimientos, cambioDeRiesgo, anticipacionEnHoras);
	}

	public static PreferenciasDeAvisosDto desde(PreferenciasDeAvisos preferencias) {
		return new PreferenciasDeAvisosDto(
				preferencias.activos(), preferencias.vencimientos(), preferencias.cambioDeRiesgo(), preferencias.anticipacionEnHoras());
	}
}
