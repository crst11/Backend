package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECPrivateKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * La identidad del servidor ante los servicios de avisos: VAPID, RFC 8292. Cada petición lleva un token
 * JWT firmado con ES256 que dice a qué servicio va, hasta cuándo vale y quién responde por el servidor.
 *
 * <p>Las llaves son las que genera cualquier herramienta VAPID: la pública de 65 bytes y la privada de
 * 32, en base64 sin relleno. Al arrancar se comprueba que sean una pareja: una firma de prueba hecha con
 * la privada tiene que verificarse con la pública. Si no, el servidor no arranca con una configuración
 * que haría fallar todos los avisos sin decir por qué.
 */
final class FirmaVapid {

	/** El token vale 12 horas: la RFC pide menos de 24. */
	private static final Duration VIGENCIA_DEL_TOKEN = Duration.ofHours(12);
	/** Quién responde por el servidor: un correo o una dirección. Sin comillas ni espacios que rompan el JSON. */
	private static final Pattern SUJETO_VALIDO = Pattern.compile("^(mailto:[^\\s\"\\\\]+|https://[^\\s\"\\\\]+)$");
	private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

	private final PrivateKey privada;
	private final String publicaEnBase64;
	private final String sujeto;
	private final Clock reloj;

	FirmaVapid(String publicaEnBase64, String privadaEnBase64, String sujeto, Clock reloj) {
		if (!SUJETO_VALIDO.matcher(sujeto == null ? "" : sujeto).matches()) {
			throw new IllegalStateException(
					"VAPID_SUJETO tiene que ser un correo (mailto:alguien@ejemplo.com) o una dirección https");
		}
		this.sujeto = sujeto;
		this.reloj = reloj;
		this.publicaEnBase64 = publicaEnBase64;
		try {
			byte[] publica = Base64.getUrlDecoder().decode(publicaEnBase64);
			byte[] escalar = Base64.getUrlDecoder().decode(privadaEnBase64);
			this.privada = KeyFactory.getInstance("EC").generatePrivate(
					new ECPrivateKeySpec(new BigInteger(1, escalar), CifradoWebPush.parametrosDeLaCurva()));
			comprobarQueSeanPareja(publica);
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new IllegalStateException(
					"Las llaves VAPID no son válidas: VAPID_PUBLICA y VAPID_PRIVADA tienen que ser una pareja P-256 en base64", e);
		}
	}

	String llavePublica() {
		return publicaEnBase64;
	}

	/** El valor de la cabecera {@code Authorization} para una petición a ese servicio de avisos. */
	String autorizacionPara(URI endpoint) {
		String servicio = endpoint.getScheme() + "://" + endpoint.getHost();
		long vence = reloj.instant().plus(VIGENCIA_DEL_TOKEN).getEpochSecond();
		String cabecera = codificar("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
		String carga = codificar("{\"aud\":\"" + servicio + "\",\"exp\":" + vence + ",\"sub\":\"" + sujeto + "\"}");
		String firmado = cabecera + "." + carga;
		return "vapid t=" + firmado + "." + BASE64.encodeToString(firmar(firmado)) + ", k=" + publicaEnBase64;
	}

	private byte[] firmar(String texto) {
		try {
			// ES256 pide la firma como R || S (64 bytes) y no en DER: de eso se encarga este formato del JDK.
			Signature firma = Signature.getInstance("SHA256withECDSAinP1363Format");
			firma.initSign(privada);
			firma.update(texto.getBytes(StandardCharsets.US_ASCII));
			return firma.sign();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo firmar el token VAPID", e);
		}
	}

	private void comprobarQueSeanPareja(byte[] publica) throws GeneralSecurityException {
		byte[] firma = firmar("prueba");
		Signature verificador = Signature.getInstance("SHA256withECDSAinP1363Format");
		verificador.initVerify(CifradoWebPush.clavePublica(publica));
		verificador.update("prueba".getBytes(StandardCharsets.US_ASCII));
		if (!verificador.verify(firma)) {
			throw new GeneralSecurityException("La llave privada no corresponde a la pública");
		}
	}

	private static String codificar(String json) {
		return BASE64.encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
