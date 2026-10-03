package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Secciones 10 y 11 de la guía: rutas públicas bajo /api/publico/**, las del estudiante bajo
 * /api/mis/** exigen un JWT de acceso, y lo demás se rechaza. CSRF solo protege las dos rutas que
 * dependen de la cookie de refresco (refresco y cierre de sesión): el resto viaja con el token en
 * el encabezado Authorization, que el navegador no envía por su cuenta.
 */
@Configuration
@EnableWebSecurity
class SeguridadConfig {

	static final String RUTA_REFRESCO = "/api/publico/auth/refresco";
	static final String RUTA_CIERRE = "/api/publico/auth/logout";

	/** La API solo devuelve JSON: nada de lo que responde debe ejecutarse ni incrustarse. */
	private static final String CSP_DE_LA_API =
			"default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

	/**
	 * Swagger UI es la excepción: no es JSON, es una página con su propio CSS y JavaScript servida
	 * por esta misma aplicación, así que con la política de la API responde 200 y se ve en blanco.
	 * Solo se permite lo que sale de este servidor; `unsafe-inline` va únicamente para estilos,
	 * porque Swagger los inyecta al dibujarse, y las imágenes aceptan `data:` por sus iconos.
	 * En producción esto no se sirve: springdoc queda deshabilitado en application-prod.properties.
	 */
	private static final String CSP_DE_SWAGGER =
			"default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
					+ "font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

	@Bean
	SecurityFilterChain filtroDeSeguridad(HttpSecurity http, CorsConfigurationSource origenesPermitidos) throws Exception {
		var rutas = PathPatternRequestMatcher.withDefaults();
		var rutasDeSwagger = new OrRequestMatcher(
				rutas.matcher("/swagger-ui/**"), rutas.matcher("/swagger-ui.html"), rutas.matcher("/v3/api-docs/**"));
		http.cors(cors -> cors.configurationSource(origenesPermitidos))
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
						// Por defecto Spring borra la cookie XSRF-TOKEN cada vez que autentica una petición con Bearer;
						// aquí el token lo emite el login y debe sobrevivir hasta el siguiente refresco.
						.sessionAuthenticationStrategy((autenticacion, peticion, respuesta) -> { })
						.requireCsrfProtectionMatcher(new OrRequestMatcher(
								rutas.matcher(HttpMethod.POST, RUTA_REFRESCO), rutas.matcher(HttpMethod.POST, RUTA_CIERRE))))
				.headers(cabeceras -> cabeceras
						.addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
								rutasDeSwagger, new ContentSecurityPolicyHeaderWriter(CSP_DE_SWAGGER)))
						.addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
								new NegatedRequestMatcher(rutasDeSwagger),
								new ContentSecurityPolicyHeaderWriter(CSP_DE_LA_API)))
						.referrerPolicy(referente -> referente.policy(
								ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
						// HSTS solo tiene efecto sobre HTTPS; en local sobre http el navegador la ignora.
						.httpStrictTransportSecurity(hsts -> hsts
								.maxAgeInSeconds(Duration.ofDays(365).toSeconds())
								.includeSubDomains(true))
						.permissionsPolicyHeader(permisos -> permisos.policy(
								"camera=(), microphone=(), geolocation=(), payment=(), usb=()")))
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(errores -> errores.accessDeniedHandler(accesoDenegadoComoProblemDetail()))
				.oauth2ResourceServer(recurso -> recurso
						.bearerTokenResolver(ignorarTokensEnRutasPublicas())
						.jwt(Customizer.withDefaults()))
				.authorizeHttpRequests(peticiones -> peticiones
						.requestMatchers("/api/publico/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/mis/**").authenticated()
						// El catálogo de programas no es de nadie en particular, pero tampoco es público.
						.requestMatchers("/api/programas/**").authenticated()
						.anyRequest().denyAll());
		return http.build();
	}

	/** Un 403 siempre (también cuando falla el token CSRF), con el mismo formato RFC 9457 del resto de la API. */
	private static AccessDeniedHandler accesoDenegadoComoProblemDetail() {
		return (peticion, respuesta, excepcion) -> {
			respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
			respuesta.setContentType("application/problem+json;charset=UTF-8");
			respuesta.getWriter().write(
					"{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"No tienes permiso para esta acción o falta el token CSRF\"}");
		};
	}

	/**
	 * Las rutas públicas no leen el encabezado Authorization: un token viejo o vencido que el
	 * frontend siga enviando no debe impedir registrarse ni iniciar sesión.
	 */
	private static BearerTokenResolver ignorarTokensEnRutasPublicas() {
		var predeterminado = new DefaultBearerTokenResolver();
		return peticion -> peticion.getRequestURI().startsWith("/api/publico/") ? null : predeterminado.resolve(peticion);
	}

	@Bean
	PasswordEncoder codificadorDeContrasenas() {
		// Fuerza 12 según la sección 11 de la guía.
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	CorsConfigurationSource origenesPermitidos(@Value("${cundiapp.cors.origenes-permitidos}") String origenes) {
		var configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(List.of(origenes.split(",")));
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
		configuracion.setAllowedHeaders(List.of("*"));
		configuracion.setAllowCredentials(true);

		var fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}
}
