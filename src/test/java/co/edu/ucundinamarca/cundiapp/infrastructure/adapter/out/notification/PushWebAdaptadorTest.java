package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort.CargaDePush;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeEnvioPush;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * El envío de Web Push (RF10, SCRUM-37) con un cliente HTTP simulado: lo que se manda, cómo viaja y cómo se
 * interpreta lo que responde el servicio de avisos.
 */
class PushWebAdaptadorTest {

	private static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/abc123";
	private static final CargaDePush CARGA = new CargaDePush("Vence pronto", "«Taller de redes» vence hoy a las 11:59 p. m.", "/pendientes");

	private final ClavesDePushDePrueba.Suscripcion navegador = ClavesDePushDePrueba.nueva();
	private final SuscripcionPush suscripcion =
			new SuscripcionPush(ENDPOINT, navegador.clavePublica(), navegador.secretoEnBase64());
	private HttpClient http;
	private PushWebAdaptador adaptador;

	@BeforeEach
	void preparar() throws Exception {
		http = mock(HttpClient.class);
		KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
		generador.initialize(new ECGenParameterSpec("secp256r1"));
		KeyPair vapid = generador.generateKeyPair();
		Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
		byte[] escalar = ((ECPrivateKey) vapid.getPrivate()).getS().toByteArray();
		byte[] relleno = new byte[32];
		System.arraycopy(escalar, Math.max(0, escalar.length - 32), relleno, 32 - Math.min(32, escalar.length), Math.min(32, escalar.length));
		FirmaVapid firma = new FirmaVapid(base64.encodeToString(ClavesDePushDePrueba.sinComprimir((ECPublicKey) vapid.getPublic())),
				base64.encodeToString(relleno), "mailto:avisos@ucundinamarca.edu.co",
				Clock.fixed(Instant.parse("2026-10-14T15:00:00Z"), ZoneOffset.UTC));
		adaptador = new PushWebAdaptador(http, firma, () -> ClavesDePushDePrueba.nueva().navegador(), () -> new byte[16]);
	}

	@SuppressWarnings("unchecked")
	private void respondeCon(int estado) throws Exception {
		HttpResponse<Void> respuesta = mock(HttpResponse.class);
		when(respuesta.statusCode()).thenReturn(estado);
		when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) respuesta);
	}

	@SuppressWarnings("unchecked")
	private HttpRequest peticionEnviada() throws Exception {
		ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
		verify(http).send(captor.capture(), any(HttpResponse.BodyHandler.class));
		return captor.getValue();
	}

	@Test
	void mandaElAvisoCifradoYFirmadoYLoDaPorEntregadoSiElServicioLoAcepta() throws Exception {
		respondeCon(201);

		ResultadoDeEnvioPush resultado = adaptador.enviar(suscripcion, CARGA, Duration.ofHours(6));

		assertThat(resultado).isEqualTo(ResultadoDeEnvioPush.ENTREGADO);
		HttpRequest peticion = peticionEnviada();
		assertThat(peticion.method()).isEqualTo("POST");
		assertThat(peticion.uri().toString()).isEqualTo(ENDPOINT);
		assertThat(peticion.headers().firstValue("Content-Encoding")).contains("aes128gcm");
		assertThat(peticion.headers().firstValue("TTL")).contains("21600");
		assertThat(peticion.headers().firstValue("Authorization").orElseThrow()).startsWith("vapid t=").contains(", k=");
	}

	@Test
	void elCuerpoSoloLoDescifraElNavegadorYTraeElFormatoQueEntiendeAngular() throws Exception {
		respondeCon(201);

		adaptador.enviar(suscripcion, CARGA, Duration.ofHours(6));

		byte[] cuerpo = leer(peticionEnviada().bodyPublisher().orElseThrow());
		String texto = new String(CifradoWebPushTest.descifrar(cuerpo, navegador.navegador(), navegador.secreto()), StandardCharsets.UTF_8);
		// En el cuerpo que viaja no se lee el aviso; solo el navegador, con sus llaves, lo descifra.
		assertThat(new String(cuerpo, StandardCharsets.ISO_8859_1)).doesNotContain("Taller de redes").doesNotContain("Vence pronto");
		assertThat(texto).startsWith("{");
		JsonNode notificacion = JsonMapper.builder().build().readTree(texto).get("notification");
		assertThat(notificacion.get("title").asString()).isEqualTo("Vence pronto");
		assertThat(notificacion.get("body").asString()).isEqualTo(CARGA.mensaje());
		assertThat(notificacion.get("icon").asString()).isEqualTo("/icons/icon-192x192.png");
		assertThat(notificacion.get("data").get("onActionClick").get("default").get("operation").asString())
				.isEqualTo("navigateLastFocusedOrOpen");
		assertThat(notificacion.get("data").get("onActionClick").get("default").get("url").asString()).isEqualTo("/pendientes");
	}

	@Test
	void unaSuscripcionQueYaNoExisteSeDistingueDeUnFalloPasajero() throws Exception {
		respondeCon(410);
		assertThat(adaptador.enviar(suscripcion, CARGA, Duration.ofHours(1))).isEqualTo(ResultadoDeEnvioPush.SUSCRIPCION_MUERTA);
	}

	@Test
	void unaSuscripcionNoEncontradaTambienEstaMuerta() throws Exception {
		respondeCon(404);
		assertThat(adaptador.enviar(suscripcion, CARGA, Duration.ofHours(1))).isEqualTo(ResultadoDeEnvioPush.SUSCRIPCION_MUERTA);
	}

	@Test
	void unErrorDelServicioOUnLimiteDeVelocidadSePuedeReintentar() throws Exception {
		for (int estado : new int[] {429, 500, 503, 401, 403, 413}) {
			respondeCon(estado);
			assertThat(adaptador.enviar(suscripcion, CARGA, Duration.ofHours(1)))
					.as("estado %d", estado).isEqualTo(ResultadoDeEnvioPush.FALLO_PASAJERO);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void sinRedONoRespondeEsUnFalloPasajero() throws Exception {
		when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("sin red"));

		assertThat(adaptador.enviar(suscripcion, CARGA, Duration.ofHours(1))).isEqualTo(ResultadoDeEnvioPush.FALLO_PASAJERO);
	}

	@Test
	void sinLlavesVapidNoHayWebPushYNoSeIntentaEnviar() {
		PushWebAdaptador sinLlaves = new PushWebAdaptador(http, null, () -> null, () -> null);

		assertThat(sinLlaves.disponible()).isFalse();
		assertThat(sinLlaves.llavePublica()).isEmpty();
		assertThatThrownBy(() -> sinLlaves.enviar(suscripcion, CARGA, Duration.ofHours(1))).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void conLlavesVapidExponeLaPublicaParaQueElNavegadorSeSuscriba() {
		assertThat(adaptador.disponible()).isTrue();
		assertThat(adaptador.llavePublica()).hasValueSatisfying(llave -> assertThat(Base64.getUrlDecoder().decode(llave)).hasSize(65));
	}

	private static byte[] leer(HttpRequest.BodyPublisher publicador) {
		List<byte[]> trozos = new ArrayList<>();
		publicador.subscribe(new Flow.Subscriber<>() {
			@Override
			public void onSubscribe(Flow.Subscription suscripcion) {
				suscripcion.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(ByteBuffer item) {
				byte[] bytes = new byte[item.remaining()];
				item.get(bytes);
				trozos.add(bytes);
			}

			@Override
			public void onError(Throwable error) {
				throw new IllegalStateException(error);
			}

			@Override
			public void onComplete() {
			}
		});
		java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
		trozos.forEach(salida::writeBytes);
		return salida.toByteArray();
	}
}
