package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

/**
 * Nombres con los que se ensambla el envío del código (SCRUM-67).
 *
 * <p>Hay dos implementaciones del mismo puerto en juego: la que entrega de verdad y el decorador que
 * la saca de la petición. En vez de confiar en un `@Primary` (que las pruebas ya usan para su propio
 * doble), cada pieza se pide por su nombre y el ensamblado queda a la vista.
 */
public final class EnvioDelCodigo {

	/** El adaptador que entrega de verdad: SMTP, consola o el doble de las pruebas. */
	public static final String DIRECTO = "envioDirectoDelCodigo";

	/** El decorador que envuelve al anterior para enviarlo fuera de la petición. */
	public static final String ASINCRONO = "enviadorDeCodigoAsincrono";

	/** Pool propio: el envío de correo no compite con los hilos que atienden peticiones. */
	public static final String EJECUTOR = "ejecutorDeEnvioDeCodigo";

	private EnvioDelCodigo() {
	}
}
