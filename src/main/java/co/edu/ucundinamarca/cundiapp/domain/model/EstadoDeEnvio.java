package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Qué pasó con la entrega de un aviso a los dispositivos (RF10). No decide si el estudiante lo ve:
 * todo aviso se ve dentro de la aplicación, haya llegado al dispositivo o no.
 */
public enum EstadoDeEnvio {
	/** Todavía no se intentó, o se intentó y falló de forma pasajera. */
	PENDIENTE("pendiente"),
	/** Llegó al servicio de avisos de al menos un dispositivo. */
	ENVIADA("enviada"),
	/** No hay dispositivo al que llegar, o se agotaron los intentos. */
	FALLIDA("fallida");

	private final String valorEnBd;

	EstadoDeEnvio(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static EstadoDeEnvio desdeBd(String valor) {
		for (EstadoDeEnvio estado : values()) {
			if (estado.valorEnBd.equals(valor)) {
				return estado;
			}
		}
		throw new ReglaDeNegocioVioladaException("No conocemos ese estado de entrega");
	}
}
