package co.edu.ucundinamarca.cundiapp.domain.model;

/** Qué respondió el servicio de avisos del navegador al intentar entregar un aviso (RF10). */
public enum ResultadoDeEnvioPush {
	/** El servicio lo aceptó: llegará al dispositivo cuando este pueda recibirlo. */
	ENTREGADO,
	/** La suscripción ya no existe (404 o 410): la persona la quitó o desinstaló la app. No se reintenta. */
	SUSCRIPCION_MUERTA,
	/** Falla pasajera del servicio o de la red: se puede reintentar más tarde. */
	FALLO_PASAJERO
}
