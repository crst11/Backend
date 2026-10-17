package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.Instant;

/**
 * Un dispositivo del estudiante que puede recibir avisos (RF10, SCRUM-37 y SCRUM-38): el teléfono o el
 * computador desde el que activó los avisos, con su suscripción Web Push.
 *
 * <p>No es lo mismo que los dispositivos de *Mis sesiones*: aquel agrupa sesiones abiertas por
 * navegador; este existe solo mientras la persona tenga los avisos activados en él.
 */
public record DispositivoConAvisos(
		int consecutivo,
		String nombre,
		SuscripcionPush suscripcion,
		boolean activo,
		Instant fechaSincronizacion) {
}
