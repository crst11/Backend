package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Instant;

/**
 * Una sesión abierta, como la ve el estudiante en *Mis sesiones* (SCRUM-49).
 * Nunca viaja la huella del token de refresco: identifica la sesión y no es asunto de la pantalla.
 */
public record SesionAbiertaDto(
		int consecutivo,
		String metodo,
		Instant fechaInicio,
		Instant fechaExpiracion,
		String dispositivo,
		String ip) {

	public static SesionAbiertaDto desde(Sesion sesion) {
		return new SesionAbiertaDto(
				sesion.consecutivo(),
				sesion.metodo().name().toLowerCase(),
				sesion.fechaInicio(),
				sesion.fechaExpiracion(),
				sesion.userAgent(),
				sesion.ipOrigen());
	}
}
