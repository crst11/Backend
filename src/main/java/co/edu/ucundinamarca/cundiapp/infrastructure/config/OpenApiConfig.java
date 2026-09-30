package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación automática de la API en /swagger-ui.html, deshabilitada en PROD (sección 10 de la guía). */
@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI documentacionApi() {
		return new OpenAPI()
				.info(new Info()
						.title("CundiApp API")
						.description("API REST de CundiApp: gestión de cuenta y sesión (RF01) y guía institucional (RF11).")
						.version("v0.1"))
				.components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}
}
