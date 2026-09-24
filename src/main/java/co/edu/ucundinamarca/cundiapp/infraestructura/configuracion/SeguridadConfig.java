package co.edu.ucundinamarca.cundiapp.infraestructura.configuracion;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
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

	@Bean
	SecurityFilterChain filtroDeSeguridad(HttpSecurity http, CorsConfigurationSource origenesPermitidos) throws Exception {
		var rutas = PathPatternRequestMatcher.withDefaults();
		http.cors(cors -> cors.configurationSource(origenesPermitidos))
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
						.requireCsrfProtectionMatcher(new OrRequestMatcher(
								rutas.matcher(HttpMethod.POST, RUTA_REFRESCO), rutas.matcher(HttpMethod.POST, RUTA_CIERRE))))
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(recurso -> recurso.jwt(Customizer.withDefaults()))
				.authorizeHttpRequests(peticiones -> peticiones
						.requestMatchers("/api/publico/**").permitAll()
						.requestMatchers("/api/mis/**").authenticated()
						.anyRequest().denyAll());
		return http.build();
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
