package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Un token de acceso solo sirve mientras su sesión siga abierta (SCRUM-77). Lo que esta prueba fija es que el token SIN el claim de
 * sesión ya no se acepta (SCRUM-227): se toleraba para los tokens anteriores a SCRUM-73, que expiraron hace semanas, y desde
 * entonces el único emisor (EmisorDeTokensJwt) siempre lo pone. Dejar la excepción era una puerta abierta sin quién la usara.
 */
class SesionSigueAbiertaTest {

	private static final Instant AHORA = Instant.parse("2026-10-19T12:00:00Z");

	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final RelojPort reloj = () -> AHORA;
	private final SesionSigueAbierta validador = new SesionSigueAbierta(sesiones, reloj);

	private static Jwt token(Map<String, Object> claims) {
		return Jwt.withTokenValue("t").header("alg", "HS256").subject("42").claims(c -> c.putAll(claims))
				.issuedAt(AHORA.minusSeconds(60)).expiresAt(AHORA.plusSeconds(600)).build();
	}

	@Test
	void aceptaElTokenDeUnaSesionVigente() {
		when(sesiones.sigueVigente(42, 7, AHORA)).thenReturn(true);

		OAuth2TokenValidatorResult resultado = validador.validate(token(Map.of("sid", 7)));

		assertThat(resultado.hasErrors()).isFalse();
	}

	@Test
	void rechazaElTokenDeUnaSesionQueYaSeCerro() {
		when(sesiones.sigueVigente(42, 7, AHORA)).thenReturn(false);

		OAuth2TokenValidatorResult resultado = validador.validate(token(Map.of("sid", 7)));

		assertThat(resultado.hasErrors()).isTrue();
		assertThat(resultado.getErrors()).singleElement().satisfies(error -> {
			assertThat(error.getErrorCode()).isEqualTo("invalid_token");
			assertThat(error.getDescription()).contains("sesión");
		});
	}

	@Test
	void rechazaElTokenSinElClaimDeSesionSinConsultarLaBaseDeDatos() {
		OAuth2TokenValidatorResult resultado = validador.validate(token(Map.of("correo", "ana@ucundinamarca.edu.co")));

		assertThat(resultado.hasErrors()).isTrue();
		verifyNoInteractions(sesiones);
	}

	@Test
	void rechazaElTokenCuyoClaimDeSesionNoEsUnNumero() {
		OAuth2TokenValidatorResult resultado = validador.validate(token(Map.of("sid", "7")));

		assertThat(resultado.hasErrors()).isTrue();
		verifyNoInteractions(sesiones);
	}
}
