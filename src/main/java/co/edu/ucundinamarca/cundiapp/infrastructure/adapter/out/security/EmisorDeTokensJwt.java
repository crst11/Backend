package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class EmisorDeTokensJwt implements EmisorDeTokensPort {

	static final String EMISOR = "cundiapp";

	private final JwtEncoder codificador;
	private final SecureRandom azar = new SecureRandom();

	EmisorDeTokensJwt(JwtEncoder codificador) {
		this.codificador = codificador;
	}

	@Override
	public TokenDeAcceso emitirAcceso(Estudiante estudiante, Instant ahora, Duration vigencia) {
		Instant expira = ahora.plus(vigencia);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(EMISOR)
				.subject(String.valueOf(estudiante.id()))
				.issuedAt(ahora)
				.expiresAt(expira)
				.claim("correo", estudiante.correo().valor())
				.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
		return new TokenDeAcceso(valor, expira);
	}

	@Override
	public String generarTokenDeRefresco() {
		byte[] bytes = new byte[32];
		azar.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
