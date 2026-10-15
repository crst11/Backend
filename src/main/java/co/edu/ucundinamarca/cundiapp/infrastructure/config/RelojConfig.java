package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj de la aplicación, con la hora de Colombia (SCRUM-85). «Hoy» y la medianoche en la que cambia
 * la pantalla del día son las de Bogotá, no las de la zona en que corra el servidor.
 */
@Configuration
class RelojConfig {

	static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

	@Bean
	Clock reloj() {
		return Clock.system(BOGOTA);
	}
}
