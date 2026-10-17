package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeEnvioPush;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Entrega un aviso a un dispositivo con Web Push (RF10, SCRUM-37): RFC 8030, con el cuerpo cifrado de
 * {@link CifradoWebPush} y la identidad de {@link FirmaVapid}.
 *
 * <p>El mensaje usa el formato que entiende el service worker de Angular, que muestra la notificación y,
 * al tocarla, abre la aplicación en la ruta indicada.
 *
 * <p>Seguridad: no sigue redirecciones (la dirección ya se validó al suscribirse y no debe cambiarse
 * sobre la marcha), no registra el contenido del aviso y de la dirección solo anota el servidor, porque el
 * resto de la ruta es un secreto que identifica al dispositivo.
 */
class PushWebAdaptador implements EnviadorDePushPort {

	private static final Logger log = LoggerFactory.getLogger(PushWebAdaptador.class);
	private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(10);
	private static final String ICONO = "/icons/icon-192x192.png";

	private final HttpClient http;
	private final FirmaVapid firma;
	private final Supplier<KeyPair> generadorDeClaves;
	private final Supplier<byte[]> generadorDeSal;
	private final JsonMapper json = JsonMapper.builder().build();

	/**
	 * @param firma nula si el servidor no tiene llaves VAPID: entonces no hay Web Push y los avisos solo
	 *     se ven dentro de la aplicación
	 * @param generadorDeClaves una pareja de llaves nueva en cada llamada (una por mensaje)
	 * @param generadorDeSal 16 bytes aleatorios nuevos en cada llamada
	 */
	PushWebAdaptador(
			HttpClient http, FirmaVapid firma, Supplier<KeyPair> generadorDeClaves, Supplier<byte[]> generadorDeSal) {
		this.http = http;
		this.firma = firma;
		this.generadorDeClaves = generadorDeClaves;
		this.generadorDeSal = generadorDeSal;
	}

	@Override
	public boolean disponible() {
		return firma != null;
	}

	@Override
	public Optional<String> llavePublica() {
		return Optional.ofNullable(firma).map(FirmaVapid::llavePublica);
	}

	@Override
	public ResultadoDeEnvioPush enviar(SuscripcionPush suscripcion, CargaDePush carga, Duration vigencia) {
		if (firma == null) {
			throw new IllegalStateException("No hay llaves VAPID: no se puede enviar Web Push");
		}
		URI endpoint = URI.create(suscripcion.endpoint());
		byte[] cuerpo = CifradoWebPush.cifrar(
				mensaje(carga), suscripcion.clavePublicaEnBytes(), suscripcion.secretoEnBytes(),
				generadorDeClaves.get(), generadorDeSal.get());
		HttpRequest peticion = HttpRequest.newBuilder(endpoint)
				.timeout(ESPERA_MAXIMA)
				.header("Authorization", firma.autorizacionPara(endpoint))
				.header("Content-Encoding", "aes128gcm")
				.header("Content-Type", "application/octet-stream")
				.header("TTL", String.valueOf(vigencia.toSeconds()))
				.header("Urgency", "normal")
				.POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
				.build();
		try {
			int estado = http.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode();
			return resultadoDe(estado, endpoint);
		} catch (IOException e) {
			log.warn("No se pudo contactar al servicio de avisos {}: {}", endpoint.getHost(), e.getClass().getSimpleName());
			return ResultadoDeEnvioPush.FALLO_PASAJERO;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ResultadoDeEnvioPush.FALLO_PASAJERO;
		}
	}

	private static ResultadoDeEnvioPush resultadoDe(int estado, URI endpoint) {
		if (estado >= 200 && estado < 300) {
			return ResultadoDeEnvioPush.ENTREGADO;
		}
		if (estado == 404 || estado == 410) {
			return ResultadoDeEnvioPush.SUSCRIPCION_MUERTA;
		}
		// 401 y 403 son las llaves VAPID rechazadas y 413 un mensaje demasiado grande: no se arreglan solos,
		// pero tampoco hay que descartar la suscripción. Se reintenta pocas veces y el aviso sigue en la app.
		log.warn("El servicio de avisos {} respondió {}", endpoint.getHost(), estado);
		return ResultadoDeEnvioPush.FALLO_PASAJERO;
	}

	private byte[] mensaje(CargaDePush carga) {
		Map<String, Object> accion = Map.of("default", Map.of("operation", "navigateLastFocusedOrOpen", "url", carga.ruta()));
		Map<String, Object> notificacion = new LinkedHashMap<>();
		notificacion.put("title", carga.titulo());
		notificacion.put("body", carga.mensaje());
		notificacion.put("icon", ICONO);
		notificacion.put("data", Map.of("onActionClick", accion));
		return json.writeValueAsString(Map.of("notification", notificacion)).getBytes(StandardCharsets.UTF_8);
	}
}
