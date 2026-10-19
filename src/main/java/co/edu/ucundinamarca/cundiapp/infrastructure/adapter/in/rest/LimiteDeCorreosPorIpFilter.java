package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadasPeticionesException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
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
 * <p>Cuenta en la base de datos, por el mismo puerto que el límite de análisis de PDF (SCRUM-216, 12 factores,
 * factor 6): el cupo de una IP no se reinicia con cada despliegue ni se multiplica con cada instancia. La clave que
 * se guarda es un HMAC de la IP, nunca la IP.
 */
@Component
class LimiteDeCorreosPorIpFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(LimiteDeCorreosPorIpFilter.class);

	static final Duration VENTANA = Duration.ofMinutes(15);
	private static final String CLAVE_DEL_LIMITE = "correos-por-ip:";

	private static final Set<String> RUTAS_QUE_ENVIAN_CORREO = Set.of(
			"/api/publico/auth/registro",
			"/api/publico/auth/verificacion/reenvio",
			"/api/publico/auth/recuperacion");

	private final LimitadorDePeticionesPort limitador;
	private final int maximoPorVentana;
	private final IpDelCliente ipDelCliente;

	LimiteDeCorreosPorIpFilter(
			LimitadorDePeticionesPort limitador,
			@Value("${cundiapp.limites.correos-por-ip:20}") int maximoPorVentana,
			@Value("${cundiapp.red.saltos-de-proxy:0}") int saltosDeProxy) {
		this.limitador = limitador;
		this.maximoPorVentana = maximoPorVentana;
		this.ipDelCliente = new IpDelCliente(saltosDeProxy);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest peticion) {
		return !HttpMethod.POST.matches(peticion.getMethod())
				|| !RUTAS_QUE_ENVIAN_CORREO.contains(peticion.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		Optional<Duration> excedido = limitador.esperaSiSeExcede(CLAVE_DEL_LIMITE + ipDelCliente.de(peticion), maximoPorVentana, VENTANA);
		if (excedido.isPresent()) {
			Duration espera = excedido.get();
			// Sin la IP ni la ruta: el log no debe servir para rastrear a nadie.
			log.warn("Se alcanzó el límite de correos por IP en una ruta pública");
			respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			respuesta.setHeader("Retry-After", String.valueOf(DemasiadasPeticionesException.segundosDeEspera(espera)));
			respuesta.setContentType("application/problem+json;charset=UTF-8");
			respuesta.getWriter().write("{\"title\":\"Demasiados intentos\",\"status\":429,"
					+ "\"detail\":\"Pediste demasiados correos seguidos. Espera " + DemasiadasPeticionesException.textoDeEspera(espera)
					+ " e intenta de nuevo\"}");
			return;
		}
		cadena.doFilter(peticion, respuesta);
	}
}
