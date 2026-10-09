package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita cuántas veces una misma IP puede pedir que se envíe un correo (SCRUM-69).
 *
 * <p>Protege las tres rutas que disparan un envío: registro, reenvío del código y recuperación de la
 * contraseña. Sin esto, un guion podría usar la app para llenar de correos la bandeja de alguien o
 * agotar la cuota del servidor de correo.
 *
 * <p>El inicio de sesión no entra aquí: ya tiene su propio límite por intentos fallidos. Verificar el
 * código tampoco: cada código admite 5 intentos y luego se bloquea solo.
 *
 * <p>Es un filtro y no una regla del dominio porque limitar por IP es un asunto del transporte: el
 * núcleo no sabe ni debe saber qué es una dirección IP. Las reglas de negocio (los 5 intentos, la
 * espera de 60 segundos entre códigos) siguen donde corresponde, en el dominio.
 *
 * <p>Cuenta en memoria de un solo proceso, igual que el límite de intentos: alcanza para un backend.
 * Con varias instancias habría que llevarlo a un almacén compartido.
 */
@Component
class LimiteDeCorreosPorIpFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(LimiteDeCorreosPorIpFilter.class);

	static final Duration VENTANA = Duration.ofMinutes(15);
	private static final int TAMANO_PARA_LIMPIAR = 10_000;

	private static final Set<String> RUTAS_QUE_ENVIAN_CORREO = Set.of(
			"/api/publico/auth/registro",
			"/api/publico/auth/verificacion/reenvio",
			"/api/publico/auth/recuperacion");

	private record Conteo(int solicitudes, Instant primera) {
	}

	private final ConcurrentHashMap<String, Conteo> conteos = new ConcurrentHashMap<>();
	private final int maximoPorVentana;
	private final IpDelCliente ipDelCliente;

	LimiteDeCorreosPorIpFilter(
			@Value("${cundiapp.limites.correos-por-ip:20}") int maximoPorVentana, IpDelCliente ipDelCliente) {
		this.maximoPorVentana = maximoPorVentana;
		this.ipDelCliente = ipDelCliente;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest peticion) {
		return !HttpMethod.POST.matches(peticion.getMethod())
				|| !RUTAS_QUE_ENVIAN_CORREO.contains(peticion.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		if (superaElLimite(ipDelCliente.de(peticion))) {
			// Sin la IP ni la ruta: el log no debe servir para rastrear a nadie.
			log.warn("Se alcanzó el límite de correos por IP en una ruta pública");
			respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			respuesta.setContentType("application/problem+json;charset=UTF-8");
			respuesta.getWriter().write("{\"title\":\"Demasiados intentos\",\"status\":429,"
					+ "\"detail\":\"Pediste demasiados correos seguidos. Espera unos minutos e intenta de nuevo\"}");
			return;
		}
		cadena.doFilter(peticion, respuesta);
	}

	private boolean superaElLimite(String ip) {
		Instant ahora = Instant.now();
		Conteo conteo = conteos.compute(ip, (k, actual) ->
				actual == null || ahora.isAfter(actual.primera().plus(VENTANA))
						? new Conteo(1, ahora)
						: new Conteo(actual.solicitudes() + 1, actual.primera()));
		if (conteos.size() > TAMANO_PARA_LIMPIAR) {
			conteos.values().removeIf(c -> ahora.isAfter(c.primera().plus(VENTANA)));
		}
		return conteo.solicitudes() > maximoPorVentana;
	}
}
