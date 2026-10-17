package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.net.URI;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/**
 * La suscripción Web Push que el navegador le da a la aplicación (RF10, SCRUM-37): a dónde mandar el
 * aviso y con qué llaves cifrarlo.
 *
 * <p>El `endpoint` lo manda el navegador, es decir, una persona. El servidor hará una petición a esa
 * dirección, así que se acepta **solo** si es HTTPS y pertenece a un servicio de avisos conocido
 * (Chrome y Edge, Firefox, Safari y Windows). Sin esta regla, una suscripción falsa serviría para que
 * el servidor llame a cualquier dirección, incluso de la red interna.
 *
 * <p>Las llaves llegan en base64 sin relleno, como las entrega el navegador: la pública del navegador
 * (65 bytes, punto sin comprimir de la curva P-256) y el secreto de autenticación (16 bytes).
 */
public record SuscripcionPush(String endpoint, String clavePublica, String secretoDeAutenticacion) {

	private static final int LARGO_MAXIMO_DEL_ENDPOINT = 500;
	private static final int LARGO_DE_LA_CLAVE_PUBLICA = 65;
	private static final int LARGO_DEL_SECRETO = 16;

	private static final Set<String> SERVICIOS_CONOCIDOS =
			Set.of("fcm.googleapis.com", "updates.push.services.mozilla.com", "web.push.apple.com");
	private static final String SUFIJO_DE_WINDOWS = ".notify.windows.com";

	public SuscripcionPush {
		if (endpoint == null || endpoint.isBlank() || endpoint.length() > LARGO_MAXIMO_DEL_ENDPOINT) {
			throw invalida();
		}
		validarEndpoint(endpoint);
		if (decodificar(clavePublica, LARGO_DE_LA_CLAVE_PUBLICA)[0] != 0x04) {
			throw invalida();
		}
		decodificar(secretoDeAutenticacion, LARGO_DEL_SECRETO);
	}

	public byte[] clavePublicaEnBytes() {
		return decodificar(clavePublica, LARGO_DE_LA_CLAVE_PUBLICA);
	}

	public byte[] secretoEnBytes() {
		return decodificar(secretoDeAutenticacion, LARGO_DEL_SECRETO);
	}

	private static void validarEndpoint(String endpoint) {
		URI uri;
		try {
			uri = URI.create(endpoint);
		} catch (IllegalArgumentException e) {
			throw invalida();
		}
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		boolean https = "https".equals(uri.getScheme());
		boolean sinCredenciales = uri.getUserInfo() == null;
		boolean puertoNormal = uri.getPort() == -1 || uri.getPort() == 443;
		if (!https || !sinCredenciales || !puertoNormal) {
			throw invalida();
		}
		if (!SERVICIOS_CONOCIDOS.contains(host) && !host.endsWith(SUFIJO_DE_WINDOWS)) {
			throw new ReglaDeNegocioVioladaException(
					"Ese navegador usa un servicio de avisos que todavía no admitimos");
		}
	}

	private static byte[] decodificar(String texto, int largoEsperado) {
		if (texto == null || texto.isBlank()) {
			throw invalida();
		}
		byte[] bytes;
		try {
			bytes = Base64.getUrlDecoder().decode(texto.replace("=", ""));
		} catch (IllegalArgumentException e) {
			throw invalida();
		}
		if (bytes.length != largoEsperado) {
			throw invalida();
		}
		return bytes;
	}

	private static ReglaDeNegocioVioladaException invalida() {
		return new ReglaDeNegocioVioladaException("La suscripción a los avisos no es válida");
	}
}
