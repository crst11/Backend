package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** La identidad VAPID del servidor (RF10, SCRUM-37): un token ES256 que cualquier servicio de avisos puede verificar. */
class FirmaVapidTest {

	private static final Instant AHORA = Instant.parse("2026-10-14T15:00:00Z");
	private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
	private static final String SUJETO = "mailto:avisos@ucundinamarca.edu.co";
	private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

	private static KeyPair parejaVapid() throws Exception {
		KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
		generador.initialize(new ECGenParameterSpec("secp256r1"));
		return generador.generateKeyPair();
	}

	private static String publica(KeyPair pareja) {
		return CODIFICADOR.encodeToString(ClavesDePushDePrueba.sinComprimir((ECPublicKey) pareja.getPublic()));
	}

	private static String privada(KeyPair pareja) {
		byte[] bytes = ((ECPrivateKey) pareja.getPrivate()).getS().toByteArray();
		byte[] relleno = new byte[32];
		System.arraycopy(bytes, Math.max(0, bytes.length - 32), relleno, 32 - Math.min(32, bytes.length), Math.min(32, bytes.length));
		return CODIFICADOR.encodeToString(relleno);
	}

	@Test
	void laCabeceraLlevaUnTokenFirmadoQueSeVerificaConLaLlavePublica() throws Exception {
		KeyPair pareja = parejaVapid();
		FirmaVapid firma = new FirmaVapid(publica(pareja), privada(pareja), SUJETO, RELOJ);

		String cabecera = firma.autorizacionPara(URI.create("https://fcm.googleapis.com/fcm/send/abc123"));

		assertThat(cabecera).startsWith("vapid t=").endsWith(", k=" + publica(pareja));
		String token = cabecera.substring("vapid t=".length(), cabecera.indexOf(", k="));
		String[] partes = token.split("\\.");
		assertThat(partes).hasSize(3);
		Signature verificador = Signature.getInstance("SHA256withECDSAinP1363Format");
		verificador.initVerify(pareja.getPublic());
		verificador.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
		assertThat(verificador.verify(DECODIFICADOR.decode(partes[2]))).isTrue();
		assertThat(DECODIFICADOR.decode(partes[2])).hasSize(64);
	}

	@Test
	void elTokenDiceAQueServicioVaHastaCuandoValeYQuienResponde() throws Exception {
		KeyPair pareja = parejaVapid();
		FirmaVapid firma = new FirmaVapid(publica(pareja), privada(pareja), SUJETO, RELOJ);

		String cabecera = firma.autorizacionPara(URI.create("https://updates.push.services.mozilla.com/wpush/v2/xyz"));
		String token = cabecera.substring("vapid t=".length(), cabecera.indexOf(", k="));
		String[] partes = token.split("\\.");

		assertThat(new String(DECODIFICADOR.decode(partes[0]), StandardCharsets.UTF_8))
				.isEqualTo("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
		assertThat(new String(DECODIFICADOR.decode(partes[1]), StandardCharsets.UTF_8))
				.isEqualTo("{\"aud\":\"https://updates.push.services.mozilla.com\",\"exp\":" + AHORA.plusSeconds(12 * 3600).getEpochSecond()
						+ ",\"sub\":\"" + SUJETO + "\"}");
	}

	@Test
	void exponeLaLlavePublicaParaQueElNavegadorSeSuscriba() throws Exception {
		KeyPair pareja = parejaVapid();

		assertThat(new FirmaVapid(publica(pareja), privada(pareja), SUJETO, RELOJ).llavePublica()).isEqualTo(publica(pareja));
	}

	@Test
	void unaPrivadaQueNoCorrespondeALaPublicaHaceFallarElArranque() throws Exception {
		KeyPair una = parejaVapid();
		KeyPair otra = parejaVapid();

		assertThatThrownBy(() -> new FirmaVapid(publica(una), privada(otra), SUJETO, RELOJ))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("llaves VAPID");
	}

	@Test
	void unasLlavesQueNoSonBase64OPuntosDeLaCurvaHacenFallarElArranque() {
		assertThatThrownBy(() -> new FirmaVapid("%%%", "%%%", SUJETO, RELOJ)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new FirmaVapid(CODIFICADOR.encodeToString(new byte[65]), CODIFICADOR.encodeToString(new byte[32]), SUJETO, RELOJ))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void elSujetoTieneQueSerUnCorreoOUnaDireccionHttpsSinCaracteresQueRompanElToken() throws Exception {
		KeyPair pareja = parejaVapid();

		assertThat(new FirmaVapid(publica(pareja), privada(pareja), "https://cundiapp.ejemplo.com/contacto", RELOJ)).isNotNull();
		for (String malo : new String[] {"", null, "avisos@ucundinamarca.edu.co", "mailto:a\"b@x.co", "mailto:a b@x.co", "http://x.co"}) {
			assertThatThrownBy(() -> new FirmaVapid(publica(pareja), privada(pareja), malo, RELOJ))
					.as("sujeto «%s»", malo).isInstanceOf(IllegalStateException.class).hasMessageContaining("VAPID_SUJETO");
		}
	}
}
