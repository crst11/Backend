package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConSesion;
import java.time.Instant;

/**
 * Un dispositivo con sesión abierta, como lo ve el estudiante en *Mis sesiones* (SCRUM-49, SCRUM-73).
 *
 * <p>Es una línea por dispositivo, no por fila de la tabla: la renovación del token abre una sesión
 * nueva cada 20 minutos y el estudiante no tiene por qué ver eso. `primerAcceso` es desde cuándo
 * ese dispositivo está dentro y `ultimoAcceso` cuándo se renovó por última vez.
 *
 * <p>Nunca viaja la huella del token de refresco: identifica la sesión y no es asunto de la pantalla.
 */
public record DispositivoConSesionDto(
		int consecutivo,
		String metodo,
		Instant primerAcceso,
		Instant ultimoAcceso,
		Instant fechaExpiracion,
		String dispositivo,
		String ip,
		boolean esLaActual) {

	public static DispositivoConSesionDto desde(DispositivoConSesion dispositivo) {
		return new DispositivoConSesionDto(
				dispositivo.consecutivo(),
				dispositivo.metodo().name().toLowerCase(),
				dispositivo.primerAcceso(),
				dispositivo.ultimoAcceso(),
				dispositivo.expira(),
				dispositivo.dispositivo(),
				dispositivo.ip(),
				dispositivo.esLaActual());
	}
}
