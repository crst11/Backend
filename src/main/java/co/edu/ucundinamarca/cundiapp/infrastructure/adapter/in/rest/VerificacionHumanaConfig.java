package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registra el filtro de la prueba de persona (SCRUM-92) con su orden: después del límite de correos por IP. */
@Configuration
class VerificacionHumanaConfig {

	@Bean
	FilterRegistrationBean<VerificacionHumanaFilter> verificacionHumana(
			VerificadorDeHumanoPort verificador,
			@Value("${cundiapp.turnstile.secreto:}") String secreto,
			@Value("${cundiapp.red.saltos-de-proxy:0}") int saltosDeProxy) {
		var registro = new FilterRegistrationBean<>(new VerificacionHumanaFilter(verificador, secreto, saltosDeProxy));
		registro.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
		return registro;
	}
}
