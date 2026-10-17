package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import java.util.Optional;

/**
 * La llave pública con la que el navegador se suscribe a los avisos (RF10). Si el servidor no tiene
 * Web Push, `disponible` es falso y `llave` viene nula: la pantalla no ofrece activar los avisos del dispositivo.
 */
public record LlaveDePushDto(boolean disponible, String llave) {

	public static LlaveDePushDto desde(Optional<String> llave) {
		return new LlaveDePushDto(llave.isPresent(), llave.orElse(null));
	}
}
