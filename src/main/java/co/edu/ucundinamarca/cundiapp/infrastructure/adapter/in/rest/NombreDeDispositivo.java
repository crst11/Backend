package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

/**
 * Cómo se llama un dispositivo en la lista de la persona, sacado del user agent: «Chrome en Android»,
 * «Safari en iOS». Es una pista para que reconozca el suyo, no una identificación.
 */
final class NombreDeDispositivo {

	static final String POR_DEFECTO = "Este dispositivo";

	private NombreDeDispositivo() {
	}

	static String desde(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return POR_DEFECTO;
		}
		String navegador = navegador(userAgent);
		String sistema = sistema(userAgent);
		return sistema == null ? navegador : navegador + " en " + sistema;
	}

	/** Edge, Opera y Chrome en iPhone dicen «Chrome» o «Safari» en su user agent: se revisa lo más específico primero. */
	private static String navegador(String ua) {
		if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) {
			return "Edge";
		}
		if (ua.contains("OPR/")) {
			return "Opera";
		}
		if (ua.contains("Firefox/") || ua.contains("FxiOS/")) {
			return "Firefox";
		}
		if (ua.contains("Chrome/") || ua.contains("CriOS/")) {
			return "Chrome";
		}
		if (ua.contains("Safari/")) {
			return "Safari";
		}
		return "Navegador";
	}

	/** Android dice también «Linux» y el iPhone dice «Mac OS X»: se revisan primero. */
	private static String sistema(String ua) {
		if (ua.contains("Android")) {
			return "Android";
		}
		if (ua.contains("iPhone") || ua.contains("iPad")) {
			return "iOS";
		}
		if (ua.contains("Windows")) {
			return "Windows";
		}
		if (ua.contains("Macintosh") || ua.contains("Mac OS X")) {
			return "macOS";
		}
		if (ua.contains("Linux")) {
			return "Linux";
		}
		return null;
	}
}
