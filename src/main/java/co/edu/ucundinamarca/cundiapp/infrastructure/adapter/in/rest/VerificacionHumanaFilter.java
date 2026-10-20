package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Exige la prueba de que quien llama es una persona antes de registrarse, iniciar sesión o pedir la
 * recuperación de la contraseña (SCRUM-92, ADR 0007).
 *
 * <p>**Apagado por defecto**: sin `TURNSTILE_SECRETO` no hace nada. La prueba viaja en el encabezado
 * `X-Verificacion-Humana`, no en el cuerpo: así este filtro no tiene que leer ni conocer el contenido de
 * la petición. Si no se puede consultar al tercero, depende de la ruta (SCRUM-233): en el inicio de sesión se deja pasar y se
 * anota (falla abierta), para que la caída de Cloudflare no deje a nadie fuera de la app; en el registro y la recuperación
 * se responde 503 (falla cerrada), porque son las dos que mandan un correo y crean cuentas, lo que un robot querría
 * aprovechar justo cuando no hay quien lo verifique.
 *
 * <p>Va después del límite de correos por IP, que es más barato, y antes de que la petición toque el
 * dominio. Es un filtro por la misma razón que ese: verificar una prueba del navegador es un asunto del
 * transporte, no una regla de negocio.
 *
 * <p>Lo registra {@link VerificacionHumanaConfig} y no es un {@code @Component}: los tests de la capa web
 * recogen todo filtro marcado así, y este necesita el verificador, que esa capa no tiene.
 */
class VerificacionHumanaFilter extends OncePerRequestFilter {

	static final String ENCABEZADO = "X-Verificacion-Humana";

	private static final Logger log = LoggerFactory.getLogger(VerificacionHumanaFilter.class);

	private static final Set<String> RUTAS_PROTEGIDAS = Set.of(
			"/api/publico/auth/registro", "/api/publico/auth/login", "/api/publico/auth/recuperacion");

	/** Las rutas que no dejan pasar si Cloudflare no responde. El login queda abierto: ahí hay una persona con cuenta esperando. */
	private static final Set<String> RUTAS_QUE_CIERRAN = Set.of(
			"/api/publico/auth/registro", "/api/publico/auth/recuperacion");

	private final VerificadorDeHumanoPort verificador;
	private final boolean activo;
	private final IpDelCliente ipDelCliente;

	VerificacionHumanaFilter(
			VerificadorDeHumanoPort verificador,
			@Value("${cundiapp.turnstile.secreto:}") String secreto,
			@Value("${cundiapp.red.saltos-de-proxy:0}") int saltosDeProxy) {
		this.verificador = verificador;
		this.activo = !secreto.isBlank();
		this.ipDelCliente = new IpDelCliente(saltosDeProxy);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest peticion) {
		return !activo
				|| !HttpMethod.POST.matches(peticion.getMethod())
				|| !RUTAS_PROTEGIDAS.contains(peticion.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		var resultado = verificador.verificar(peticion.getHeader(ENCABEZADO), ipDelCliente.de(peticion));
		switch (resultado) {
			case HUMANO -> cadena.doFilter(peticion, respuesta);
			case NO_DISPONIBLE -> {
				if (RUTAS_QUE_CIERRAN.contains(peticion.getRequestURI())) {
					log.warn("No se pudo verificar la prueba de persona; se rechaza {}", peticion.getRequestURI());
					responder(respuesta, HttpStatus.SERVICE_UNAVAILABLE, "Verificación no disponible",
							"No pudimos verificar que eres una persona en este momento. Intenta de nuevo en unos minutos");
					return;
				}
				log.warn("No se pudo verificar la prueba de persona; se deja pasar");
				cadena.doFilter(peticion, respuesta);
			}
			case ROBOT -> {
				log.info("Petición pública rechazada: falta la prueba de persona o no es válida");
				responder(respuesta, HttpStatus.UNPROCESSABLE_ENTITY, "Verificación pendiente",
						"No pudimos confirmar que eres una persona. Recarga la página e intenta de nuevo");
			}
		}
	}

	private static void responder(HttpServletResponse respuesta, HttpStatus estado, String titulo, String detalle)
			throws IOException {
		respuesta.setStatus(estado.value());
		respuesta.setContentType("application/problem+json;charset=UTF-8");
		respuesta.getWriter().write("{\"title\":\"" + titulo + "\",\"status\":" + estado.value()
				+ ",\"detail\":\"" + detalle + "\"}");
	}
}
