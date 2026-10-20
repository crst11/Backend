package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Rechaza un token de acceso cuya sesión ya se cerró (SCRUM-77).
 *
 * <p>Antes, cerrar una sesión solo revocaba la fila: el token de acceso seguía sirviendo hasta 20
 * minutos porque el JWT es sin estado y nadie comprobaba nada. Para el botón de *cerrar todas las
 * sesiones* eso era grave, porque quien lo toca suele sospechar que alguien más entró a su cuenta,
 * y ese alguien conservaba el acceso veinte minutos más.
 *
 * <p>Esto cuesta una búsqueda por clave primaria en cada petición protegida. Es el precio de que
 * cerrar sesión signifique cerrar sesión.
 */
@Component
public class SesionSigueAbierta implements OAuth2TokenValidator<Jwt> {

	private final SesionRepositorio sesiones;
	private final RelojPort reloj;

	SesionSigueAbierta(SesionRepositorio sesiones, RelojPort reloj) {
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (!(token.getClaim(EmisorDeTokensJwt.SESION) instanceof Number consecutivo)) {
			// Un token sin el claim de sesión no se puede atar a ninguna fila de sesion, así que no hay forma de saber si sigue
			// abierta. Antes se aceptaba por los tokens emitidos antes de SCRUM-73, que duraban 20 minutos y expiraron hace
			// semanas; el único emisor (EmisorDeTokensJwt) siempre lo pone (SCRUM-227).
			return OAuth2TokenValidatorResult.failure(
					new OAuth2Error("invalid_token", "El token no indica a qué sesión pertenece", null));
		}
		if (sesiones.sigueVigente(Integer.parseInt(token.getSubject()), consecutivo.intValue(), reloj.ahora())) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(
				new OAuth2Error("invalid_token", "La sesión de este token ya se cerró", null));
	}
}
