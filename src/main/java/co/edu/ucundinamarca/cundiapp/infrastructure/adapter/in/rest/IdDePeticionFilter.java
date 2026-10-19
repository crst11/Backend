package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Le da a cada petición un identificador y lo deja en todas las líneas del registro que se escriban mientras se
 * atiende (12 factores, factor 11, SCRUM-213). Con los registros estructurados de la nube, buscar ese id reúne lo que
 * pasó en una sola petición, y el estudiante o quien dé soporte lo puede citar: se devuelve en el encabezado
 * {@code X-Request-Id}.
 *
 * <p>El id lo genera nginx de borde ({@code $request_id}) y llega en el mismo encabezado; si falta, se crea uno aquí.
 * Un id que trae el cliente solo se acepta si es corto y de caracteres inocuos: un salto de línea o un texto largo en
 * un encabezado se volvería una línea falsa en el registro. No lleva nada de la persona.
 *
 * <p>Va antes que Spring Security para que también queden marcados los rechazos de seguridad.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdDePeticionFilter extends OncePerRequestFilter {

	static final String ENCABEZADO = "X-Request-Id";
	/** El nombre con el que aparece en cada línea del registro estructurado. */
	static final String CLAVE_EN_EL_REGISTRO = "peticion";

	private static final Pattern VALIDO = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		String id = elegir(peticion.getHeader(ENCABEZADO));
		MDC.put(CLAVE_EN_EL_REGISTRO, id);
		respuesta.setHeader(ENCABEZADO, id);
		try {
			cadena.doFilter(peticion, respuesta);
		} finally {
			MDC.remove(CLAVE_EN_EL_REGISTRO);
		}
	}

	static String elegir(String recibido) {
		return recibido != null && VALIDO.matcher(recibido).matches() ? recibido : UUID.randomUUID().toString();
	}
}
