package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.CrearMiPendiente;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.PrioridadDePendiente;
import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * El pendiente que manda la pantalla (RF06, SCRUM-29). Solo se exige que el cuerpo sea válido: que haya
 * título, que quepa y que haya fecha lo comprueba el dominio, para que el mensaje sea el mismo venga de
 * donde venga.
 *
 * @param fechaLimite con su zona, por ejemplo {@code 2026-10-20T23:59:00-05:00}
 * @param prioridad {@code alta}, {@code media} o {@code baja}; vacía queda en media
 * @param idMatricula la asignatura, si la quiere asociar
 */
public record CrearPendienteDto(
		String titulo, String descripcion, OffsetDateTime fechaLimite, String prioridad, Integer idMatricula) {

	public CrearMiPendiente.Datos aDatos() {
		return new CrearMiPendiente.Datos(
				titulo, descripcion, fechaLimite == null ? null : fechaLimite.toInstant(), prioridadElegida(), idMatricula);
	}

	private PrioridadDePendiente prioridadElegida() {
		if (prioridad == null || prioridad.isBlank()) {
			return null;
		}
		try {
			return PrioridadDePendiente.desdeBd(prioridad.strip().toLowerCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new ReglaDeNegocioVioladaException("La prioridad tiene que ser alta, media o baja");
		}
	}
}
