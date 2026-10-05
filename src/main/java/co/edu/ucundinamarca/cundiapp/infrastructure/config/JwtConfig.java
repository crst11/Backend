package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security.SesionSigueAbierta;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Firma y valida los tokens de acceso con HMAC-SHA256 y el secreto que entrega el entorno (JWT_SECRETO). */
@Configuration
class JwtConfig {

	private static final String EMISOR = "cundiapp";

	@Bean
	SecretKey claveDeFirma(@Value("${cundiapp.jwt.secreto}") String secreto) {
		byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException(
					"JWT_SECRETO debe tener al menos 32 caracteres. Si está vacío en tu .env, bórralo o dale un valor.");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey claveDeFirma) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(claveDeFirma));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey claveDeFirma, SesionSigueAbierta sesionSigueAbierta) {
		NimbusJwtDecoder decodificador =
				NimbusJwtDecoder.withSecretKey(claveDeFirma).macAlgorithm(MacAlgorithm.HS256).build();
		// Además de la firma y el vencimiento, se comprueba que la sesión del token siga abierta:
		// si no, cerrar sesión no cortaría el acceso hasta que venciera el token (SCRUM-77).
		decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(EMISOR), sesionSigueAbierta));
		return decodificador;
	}
}
