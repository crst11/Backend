package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * La IP del estudiante que hace la petición, con la que se limitan los intentos (SCRUM-71).
 *
 * <p>Detrás de CloudFront y el balanceador, {@code getRemoteAddr()} es la IP del balanceador: la
 * misma para todos. Los límites por IP compartirían un solo cupo y quien lo agotara dejaría fuera a
 * todos los estudiantes.
 *
 * <p>{@code X-Forwarded-For} sí trae la IP real, pero el cliente también puede escribirlo, así que
 * no se lee a ciegas. Cada proxy agrega al final del encabezado la IP de quien le habló: con N
 * proxies de confianza, la IP del cliente es la N-ésima contando desde el final. Lo que el cliente
 * haya puesto antes queda a la izquierda y nunca se mira.
 *
 * <p>{@code cundiapp.red.saltos-de-proxy} es cuántos proxies hay delante de la aplicación: 0 en
 * local (se usa la IP de la conexión y el encabezado se ignora), 1 detrás de un balanceador y 2
 * detrás de CloudFront más el balanceador.
 */
@Component
class IpDelCliente {

	private static final String ENCABEZADO = "X-Forwarded-For";
	/** La IPv6 más larga con IPv4 incrustada mide 45 caracteres. */
	private static final int LARGO_MAXIMO = 45;
	private static final Pattern PARECE_UNA_IP = Pattern.compile("[0-9a-fA-F:.]+");

	private final int saltosDeProxy;

	IpDelCliente(@Value("${cundiapp.red.saltos-de-proxy:0}") int saltosDeProxy) {
		if (saltosDeProxy < 0) {
			throw new IllegalArgumentException("cundiapp.red.saltos-de-proxy no puede ser negativo");
		}
		this.saltosDeProxy = saltosDeProxy;
	}

	String de(HttpServletRequest peticion) {
		String conexion = peticion.getRemoteAddr();
		if (saltosDeProxy == 0) {
			return conexion;
		}
		String encabezado = peticion.getHeader(ENCABEZADO);
		if (encabezado == null || encabezado.isBlank()) {
			return conexion;
		}
		String[] entradas = Arrays.stream(encabezado.split(",")).map(String::trim).toArray(String[]::new);
		if (entradas.length < saltosDeProxy) {
			// Faltan entradas: alguien se saltó un proxy. No se inventa un cliente.
			return conexion;
		}
		String candidata = entradas[entradas.length - saltosDeProxy];
		return parece(candidata) ? candidata : conexion;
	}

	/**
	 * No es una validación completa de direcciones, y no necesita serlo: solo acota lo que puede
	 * terminar como llave de un contador. Un texto cualquiera crearía un cupo nuevo por petición.
	 */
	private static boolean parece(String texto) {
		return !texto.isEmpty()
				&& texto.length() <= LARGO_MAXIMO
				&& PARECE_UNA_IP.matcher(texto).matches()
				&& (texto.indexOf('.') >= 0 || texto.indexOf(':') >= 0);
	}
}
