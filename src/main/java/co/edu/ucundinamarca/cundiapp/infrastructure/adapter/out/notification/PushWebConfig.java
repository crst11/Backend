package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma el envío de Web Push con las llaves VAPID del entorno (RF10, SCRUM-37). Sin ellas, o con unas
 * inválidas, no falla: deja el envío desactivado y los avisos se ven solo dentro de la aplicación. Un valor
 * VAPID mal puesto no puede tumbar el servicio entero (SCRUM-203).
 */
@Configuration
class PushWebConfig {

	private static final Logger log = LoggerFactory.getLogger(PushWebConfig.class);
	private static final Duration ESPERA_PARA_CONECTAR = Duration.ofSeconds(5);

	@Bean
	EnviadorDePushPort enviadorDePush(
			@Value("${cundiapp.push.vapid-publica:}") String publica,
			@Value("${cundiapp.push.vapid-privada:}") String privada,
			@Value("${cundiapp.push.sujeto:}") String sujeto,
			Clock reloj) {
		HttpClient http = HttpClient.newBuilder()
				.connectTimeout(ESPERA_PARA_CONECTAR)
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
		FirmaVapid firma = firmaDeLasLlaves(publica, privada, sujeto, reloj);
		SecureRandom azar = new SecureRandom();
		return new PushWebAdaptador(http, firma, PushWebConfig::nuevaClaveEfimera, () -> {
			byte[] sal = new byte[CifradoWebPush.LARGO_DE_LA_SAL];
			azar.nextBytes(sal);
			return sal;
		});
	}

	/** La firma VAPID, o {@code null} si no hay llaves o no sirven. Nunca registra un valor, solo el motivo. */
	static FirmaVapid firmaDeLasLlaves(String publica, String privada, String sujeto, Clock reloj) {
		if (publica.isBlank() || privada.isBlank()) {
			log.info("Web Push desactivado: faltan VAPID_PUBLICA y VAPID_PRIVADA. Los avisos se ven dentro de la aplicación.");
			return null;
		}
		try {
			return new FirmaVapid(publica, privada, sujeto, reloj);
		} catch (IllegalStateException e) {
			log.error("Web Push desactivado porque la configuración VAPID no es válida: {}. Los avisos se ven dentro de la aplicación.",
					e.getMessage());
			return null;
		}
	}

	private static java.security.KeyPair nuevaClaveEfimera() {
		try {
			KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
			generador.initialize(new ECGenParameterSpec("secp256r1"));
			return generador.generateKeyPair();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo generar la llave efímera de Web Push", e);
		}
	}
}
