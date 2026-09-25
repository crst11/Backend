package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.SesionIniciada;
import java.time.Duration;
import java.time.Instant;

/** El token de acceso viaja en el cuerpo; el de refresco nunca: va en una cookie HttpOnly. */
public record SesionDto(String tokenDeAcceso, String tipo, long expiraEnSegundos, MiCuentaDto cuenta) {

	public static SesionDto desde(SesionIniciada sesion, Instant ahora) {
		return new SesionDto(
				sesion.tokenDeAcceso(),
				"Bearer",
				Duration.between(ahora, sesion.accesoExpira()).toSeconds(),
				MiCuentaDto.desde(sesion.estudiante()));
	}
}
