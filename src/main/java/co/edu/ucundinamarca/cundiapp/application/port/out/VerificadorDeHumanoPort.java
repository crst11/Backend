package co.edu.ucundinamarca.cundiapp.application.port.out;

/**
 * Comprueba que quien hace una petición pública es una persona y no un guion (SCRUM-92). Lo que hay
 * detrás (hoy Cloudflare Turnstile) es un detalle del adaptador; el núcleo solo sabe de tres respuestas.
 */
public interface VerificadorDeHumanoPort {

	enum Resultado {
		HUMANO,
		/** El tercero rechazó la prueba, o no hubo prueba. */
		ROBOT,
		/** No se pudo preguntar (sin red, tiempo agotado, respuesta ilegible): no es culpa de quien pide. */
		NO_DISPONIBLE
	}

	Resultado verificar(String prueba, String ip);
}
