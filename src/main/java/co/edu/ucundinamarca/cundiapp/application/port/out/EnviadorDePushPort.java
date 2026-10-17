package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeEnvioPush;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.time.Duration;
import java.util.Optional;

/**
 * Lo que el núcleo necesita para entregar un aviso a un dispositivo aunque la aplicación esté cerrada
 * (RF10, SCRUM-37): Web Push. Es un extra sobre el aviso dentro de la aplicación, no una condición.
 */
public interface EnviadorDePushPort {

	/** Falso si el servidor no tiene llaves VAPID: los avisos siguen viéndose dentro de la aplicación. */
	boolean disponible();

	/** La llave pública VAPID, en base64 sin relleno, para que el navegador se suscriba. Vacía si no está disponible. */
	Optional<String> llavePublica();

	/**
	 * @param vigencia cuánto tiempo guarda el servicio de avisos del navegador el mensaje si el dispositivo
	 *     no está en línea; pasado ese tiempo lo descarta, porque un «vence pronto» tardío ya no sirve
	 */
	ResultadoDeEnvioPush enviar(SuscripcionPush suscripcion, CargaDePush carga, Duration vigencia);

	/** Lo que muestra la notificación del dispositivo y a dónde lleva al tocarla. */
	record CargaDePush(String titulo, String mensaje, String ruta) {
	}
}
