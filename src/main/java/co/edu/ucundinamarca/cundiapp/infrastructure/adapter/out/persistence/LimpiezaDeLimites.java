package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Borra de {@code limite_de_uso} lo que ya no puede bloquear a nadie (12 factores, factor 6, SCRUM-216). Todos los límites
 * duran como mucho 15 minutos, así que lo que lleva más de un día sin tocarse sobra. El mapa en memoria se limpiaba solo
 * al reiniciar; una tabla no.
 *
 * <p>Corre en cada instancia y no pasa nada: borrar dos veces lo mismo es inofensivo. Se apaga junto con las demás tareas
 * programadas ({@code cundiapp.avisos.programados=false}), como en las pruebas.
 */
@Component
@ConditionalOnProperty(name = "cundiapp.avisos.programados", havingValue = "true", matchIfMissing = true)
class LimpiezaDeLimites {

	private static final Logger log = LoggerFactory.getLogger(LimpiezaDeLimites.class);

	/** Cuánto se conserva una fila sin tocarse. Mucho más que la ventana más larga (15 minutos). */
	static final Duration CONSERVAR = Duration.ofDays(1);

	private final JdbcTemplate jdbc;
	private final RelojPort reloj;

	LimpiezaDeLimites(JdbcTemplate jdbc, RelojPort reloj) {
		this.jdbc = jdbc;
		this.reloj = reloj;
	}

	@Scheduled(fixedDelayString = "${cundiapp.limites.limpieza-cada:PT1H}", initialDelayString = "${cundiapp.limites.limpieza-espera-inicial:PT5M}")
	void limpiar() {
		try {
			int borradas = limpiarLoAnteriorA(reloj.ahora().minus(CONSERVAR));
			if (borradas > 0) {
				log.info("Limpieza de límites: {} filas borradas", borradas);
			}
		} catch (RuntimeException e) {
			log.error("La limpieza de límites falló; se reintenta en la siguiente ronda", e);
		}
	}

	/** Borra las filas cuya última vez fue antes de {@code limite}; devuelve cuántas. */
	int limpiarLoAnteriorA(Instant limite) {
		return jdbc.update("DELETE FROM limite_de_uso WHERE ultima < ?", OffsetDateTime.ofInstant(limite, ZoneOffset.UTC));
	}
}
