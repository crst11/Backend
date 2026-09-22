package co.edu.ucundinamarca.cundiapp.infraestructura.configuracion;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Reglas de la sección 9.4 de la guía: rutas públicas bajo /api/publico/**, todo lo demás exige
 * autenticación. El inicio de sesión con JWT llega con RF01 (CUN-19 a CUN-22); hasta entonces no
 * hay ninguna ruta autenticada que probar, así que cualquier otra petición se rechaza.
 */
@Configuration
@EnableWebSecurity
class SeguridadConfig {

	@Bean
	SecurityFilterChain filtroDeSeguridad(HttpSecurity http, CorsConfigurationSource origenesPermitidos) throws Exception {
		http.cors(cors -> cors.configurationSource(origenesPermitidos))
				.csrf(csrf -> csrf.disable()) // se activa solo para la cookie de refresco cuando exista (RF01)
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(peticiones -> peticiones
						.requestMatchers("/api/publico/**").permitAll()
						.anyRequest().denyAll());
		return http.build();
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
