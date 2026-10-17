package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import java.time.Instant;

/** Un dispositivo que recibe avisos (RF10). No expone la dirección ni las llaves de la suscripción: son secretas. */
public record DispositivoConAvisosDto(int consecutivo, String nombre, boolean activo, Instant fechaSincronizacion) {

	public static DispositivoConAvisosDto desde(DispositivoConAvisos dispositivo) {
		return new DispositivoConAvisosDto(
				dispositivo.consecutivo(), dispositivo.nombre(), dispositivo.activo(), dispositivo.fechaSincronizacion());
	}
}
