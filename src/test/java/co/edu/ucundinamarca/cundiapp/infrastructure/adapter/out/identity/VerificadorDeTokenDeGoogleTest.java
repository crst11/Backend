package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.IdentidadExternaInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ServicioExternoNoDisponibleException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Las mismas reglas que se aplican a los tokens de Google, pero con un par de llaves RSA generado en
 * la prueba: no se llama a Google y se puede fabricar cada caso (vencido, de otra app, mal firmado...).
 */
class VerificadorDeTokenDeGoogleTest {

	private static final String CLIENT_ID = "123-cundiapp.apps.googleusercontent.com";

	private final RSAKey llaveDeGoogle = generarLlave();
	private final RSAKey llaveDeUnImpostor = generarLlave();
	private final VerificadorDeTokenDeGoogle verificador = verificadorCon(llaveDeGoogle);

	private static RSAKey generarLlave() {
		try {
			return new RSAKeyGenerator(2048).keyID("llave-de-prueba").generate();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static VerificadorDeTokenDeGoogle verificadorCon(RSAKey llavePublica) {
		try {
			NimbusJwtDecoder decodificador = NimbusJwtDecoder.withPublicKey(llavePublica.toRSAPublicKey()).build();
			decodificador.setJwtValidator(VerificadorDeTokenDeGoogle.reglasDeGoogle(CLIENT_ID));
			return new VerificadorDeTokenDeGoogle(decodificador);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String token(RSAKey firmante, Consumer<JwtClaimsSet.Builder> ajuste) {
		Instant ahora = Instant.now();
		var claims = JwtClaimsSet.builder()
				.issuer("https://accounts.google.com")
				.audience(List.of(CLIENT_ID))
				.subject("1098765")
				.issuedAt(ahora)
				.expiresAt(ahora.plusSeconds(3600))
				.claim("email", "ana.diaz@gmail.com")
				.claim("email_verified", true);
		ajuste.accept(claims);
		var codificador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(firmante)));
		return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build()))
				.getTokenValue();
	}

	@Test
	void aceptaUnTokenDeGoogleParaNuestraAppYDiceQuienEs() {
		var identidad = verificador.verificar(token(llaveDeGoogle, claims -> { }));

		assertThat(identidad.identificador()).isEqualTo("1098765");
		assertThat(identidad.correo()).isEqualTo("ana.diaz@gmail.com");
		assertThat(identidad.correoVerificado()).isTrue();
	}

	@Test
	void aceptaLosDosEmisoresQueUsaGoogle() {
		var identidad = verificador.verificar(token(llaveDeGoogle, claims -> claims.issuer("accounts.google.com")));

		assertThat(identidad.identificador()).isEqualTo("1098765");
	}

	@Test
	void rechazaUnTokenEmitidoParaOtraApp() {
		String deOtraApp = token(llaveDeGoogle, claims -> claims.audience(List.of("otra-app.apps.googleusercontent.com")));

		assertThatThrownBy(() -> verificador.verificar(deOtraApp)).isInstanceOf(IdentidadExternaInvalidaException.class);
	}

	@Test
	void rechazaUnTokenQueNoEmitioGoogle() {
		String deOtroEmisor = token(llaveDeGoogle, claims -> claims.issuer("https://login.impostor.com"));

		assertThatThrownBy(() -> verificador.verificar(deOtroEmisor)).isInstanceOf(IdentidadExternaInvalidaException.class);
	}

	@Test
	void rechazaUnTokenVencido() {
		Instant antes = Instant.now().minusSeconds(7200);
		String vencido = token(llaveDeGoogle, claims -> claims.issuedAt(antes).expiresAt(antes.plusSeconds(3600)));

		assertThatThrownBy(() -> verificador.verificar(vencido)).isInstanceOf(IdentidadExternaInvalidaException.class);
	}

	@Test
	void rechazaUnTokenFirmadoConOtraLlave() {
		String falsificado = token(llaveDeUnImpostor, claims -> { });

		assertThatThrownBy(() -> verificador.verificar(falsificado)).isInstanceOf(IdentidadExternaInvalidaException.class);
	}

	@Test
	void rechazaTextoQueNiSiquieraEsUnToken() {
		assertThatThrownBy(() -> verificador.verificar("no-es-un-jwt")).isInstanceOf(IdentidadExternaInvalidaException.class);
	}

	@Test
	void informaSiGoogleNoVerificoElCorreo() {
		var identidad = verificador.verificar(token(llaveDeGoogle, claims -> claims.claim("email_verified", false)));

		assertThat(identidad.correoVerificado()).isFalse();
	}

	@Test
	void sinClientIdConfiguradoGoogleNoEstaDisponible() {
		var sinConfigurar = new VerificadorDeTokenDeGoogle("");

		assertThatThrownBy(() -> sinConfigurar.verificar("cualquier-token"))
				.isInstanceOf(ServicioExternoNoDisponibleException.class);
	}

	@Test
	void siNoSePuedenDescargarLasLlavesDeGoogleEsUnServicioNoDisponibleYNoUnTokenMalo() {
		var sinConexion = new VerificadorDeTokenDeGoogle(token -> {
			throw new JwtException("Couldn't retrieve remote JWK set");
		});

		assertThatThrownBy(() -> sinConexion.verificar("un-token"))
				.isInstanceOf(ServicioExternoNoDisponibleException.class)
				.hasMessageContaining("Google");
	}
}
