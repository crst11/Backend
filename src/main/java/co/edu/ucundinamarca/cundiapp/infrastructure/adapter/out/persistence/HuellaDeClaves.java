package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Convierte lo que se cuenta en un límite (un correo, una IP, un estudiante) en la clave con que se guarda en
 * {@code limite_de_uso} (12 factores, factor 6, SCRUM-216). Es un HMAC-SHA256 en hexadecimal: 64 caracteres que no dejan
 * ver el correo ni la IP a quien lea la tabla o una copia de seguridad.
 *
 * <p>Es un HMAC y no un hash simple porque el espacio de direcciones IPv4 es tan chico que un hash simple se
 * revertiría probándolas todas; sin el secreto del servidor no se puede. Lleva el espacio del límite como parte de lo
 * que se firma: la misma IP en dos límites distintos da dos claves distintas.
 */
@Component
class HuellaDeClaves {

	private static final String ALGORITMO = "HmacSHA256";
	private static final String SEPARADOR = "\u0000";

	private final SecretKeySpec llave;

	HuellaDeClaves(@Value("${cundiapp.jwt.secreto}") String secreto) {
		this.llave = new SecretKeySpec(derivar(secreto, "limites-de-uso"), ALGORITMO);
	}

	/**
	 * La llave de las huellas sale del secreto con HMAC (una función de una vía), no de pegarle un prefijo: con el prefijo, quien
	 * conociera la llave de las huellas conocería el secreto que firma los tokens (SCRUM-227). Así cada uso (firmar, hacer huellas)
	 * tiene su propia llave y la de uno no revela la del otro.
	 */
	private static byte[] derivar(String secreto, String proposito) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
			return mac.doFinal(("cundiapp/llave-derivada/" + proposito).getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo derivar la llave de las huellas", e);
		}
	}

	/** Los 64 caracteres hexadecimales de {@code espacio} + {@code clave}. */
	String de(String espacio, String clave) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(llave);
			return HexFormat.of().formatHex(mac.doFinal((espacio + SEPARADOR + clave).getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo calcular la clave de un límite", e);
		}
	}
}
