package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.identity;

import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeIdentidadExternaPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.IdentidadExternaInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ServicioExternoNoDisponibleException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * API externa de Google (Google Identity Services). El frontend obtiene un ID token con el botón
 * "Continuar con Google" y aquí se comprueba sin confiar en él: firma RS256 con las llaves públicas
 * que Google publica (se descargan y se guardan en caché), emisor de Google, que el destinatario sea
 * nuestro client ID y que no haya vencido. No hace falta client secret: solo se valida el token.
 */
@Component
class VerificadorDeTokenDeGoogle implements VerificadorDeIdentidadExternaPort {

	private static final Logger log = LoggerFactory.getLogger(VerificadorDeTokenDeGoogle.class);

	static final String LLAVES_PUBLICAS = "https://www.googleapis.com/oauth2/v3/certs";
	static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");
	private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(3);

	private final JwtDecoder decodificador;

	@Autowired
	VerificadorDeTokenDeGoogle(@Value("${cundiapp.google.client-id:}") String clientId) {
		this(clientId.isBlank() ? null : decodificadorContraGoogle(clientId));
	}

	/** Para las pruebas: un decodificador con otras llaves pero las mismas reglas de validación. */
	VerificadorDeTokenDeGoogle(JwtDecoder decodificador) {
		this.decodificador = decodificador;
	}

	@Override
	public IdentidadExterna verificar(String idToken) {
		if (decodificador == null) {
			log.warn("Se intentó usar Google sin GOOGLE_CLIENT_ID configurado");
			throw new ServicioExternoNoDisponibleException("El inicio con Google no está disponible en este momento");
		}
		Jwt token;
		try {
			token = decodificador.decode(idToken);
		} catch (BadJwtException e) {
			log.info("Token de Google rechazado: {}", e.getMessage());
			throw new IdentidadExternaInvalidaException();
		} catch (JwtException e) {
			// No es un token malo: no se pudieron descargar las llaves de Google.
			log.warn("No se pudo consultar a Google para validar el token ({})", e.getClass().getSimpleName());
			throw new ServicioExternoNoDisponibleException("No pudimos comunicarnos con Google. Intenta de nuevo en unos minutos", e);
		}

		String correo = token.getClaimAsString("email");
		if (token.getSubject() == null || correo == null || correo.isBlank()) {
			throw new IdentidadExternaInvalidaException();
		}
		return new IdentidadExterna(token.getSubject(), correo, Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified")));
	}

	static OAuth2TokenValidator<Jwt> reglasDeGoogle(String clientId) {
		return new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtClaimValidator<String>(JwtClaimNames.ISS, EMISORES::contains),
				new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, destinatarios ->
						destinatarios != null && destinatarios.contains(clientId)));
	}

	private static JwtDecoder decodificadorContraGoogle(String clientId) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(ESPERA_MAXIMA);
		fabrica.setReadTimeout(ESPERA_MAXIMA);
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(LLAVES_PUBLICAS)
				.restOperations(new RestTemplate(fabrica))
				.build();
		decodificador.setJwtValidator(reglasDeGoogle(clientId));
		return decodificador;
	}
}
