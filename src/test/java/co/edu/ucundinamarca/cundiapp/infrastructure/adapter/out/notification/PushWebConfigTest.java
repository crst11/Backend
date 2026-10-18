package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Una configuración VAPID mala desactiva Web Push y no tumba el servicio (SCRUM-203). */
class PushWebConfigTest {

	private static final Clock RELOJ = Clock.systemUTC();
	private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
	private static final String SUJETO = "mailto:avisos@ucundinamarca.edu.co";

	private final ListAppender<ILoggingEvent> registro = new ListAppender<>();
	private final Logger logger = (Logger) LoggerFactory.getLogger(PushWebConfig.class);

	@BeforeEach
	void escucharElRegistro() {
		registro.start();
		logger.addAppender(registro);
	}

	@AfterEach
	void dejarDeEscuchar() {
		logger.detachAppender(registro);
	}

	private static KeyPair pareja() throws Exception {
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
	void conLlavesYSujetoValidosLaFirmaQuedaActiva() throws Exception {
		KeyPair par = pareja();

		assertThat(PushWebConfig.firmaDeLasLlaves(publica(par), privada(par), SUJETO, RELOJ)).isNotNull();
	}

	@Test
	void sinLlavesNoHayFirmaYNoEsUnError() {
		assertThat(PushWebConfig.firmaDeLasLlaves("", "", "", RELOJ)).isNull();
		assertThat(registro.list).noneMatch(e -> e.getLevel().isGreaterOrEqual(ch.qos.logback.classic.Level.ERROR));
	}

	@Test
	void unSujetoSinMailtoDesactivaWebPushEnVezDeTumbarElArranque() throws Exception {
		KeyPair par = pareja();

		assertThat(PushWebConfig.firmaDeLasLlaves(publica(par), privada(par), "avisos@ucundinamarca.edu.co", RELOJ)).isNull();
		assertThat(registro.list).anyMatch(e -> e.getFormattedMessage().contains("VAPID_SUJETO"));
	}

	@Test
	void llavesQueNoSonParejaDesactivanWebPushSinImprimirSusValores() throws Exception {
		KeyPair una = pareja();
		KeyPair otra = pareja();
		String publicaDeUna = publica(una);
		String privadaDeOtra = privada(otra);

		assertThat(PushWebConfig.firmaDeLasLlaves(publicaDeUna, privadaDeOtra, SUJETO, RELOJ)).isNull();
		assertThat(registro.list).isNotEmpty().allSatisfy(e -> {
			assertThat(e.getFormattedMessage()).doesNotContain(publicaDeUna).doesNotContain(privadaDeOtra);
			assertThat(e.getThrowableProxy()).isNull();
		});
	}

	@Test
	void llavesQueNoSonBase64DesactivanWebPush() {
		assertThat(PushWebConfig.firmaDeLasLlaves("%%%", "%%%", SUJETO, RELOJ)).isNull();
	}
}
