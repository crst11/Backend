package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * La suscripción tal como la entrega el navegador con {@code PushSubscription.toJSON()}: la dirección y
 * las dos llaves. Solo se exige que vengan; que la dirección sea de un servicio de avisos conocido y que
 * las llaves tengan el tamaño de P-256 lo comprueba el dominio.
 */
public record SuscripcionPushDto(@NotBlank String endpoint, @NotNull @Valid Llaves keys) {

	public record Llaves(@NotBlank String p256dh, @NotBlank String auth) {
	}

	public SuscripcionPush aSuscripcion() {
		return new SuscripcionPush(endpoint, keys.p256dh(), keys.auth());
	}
}
