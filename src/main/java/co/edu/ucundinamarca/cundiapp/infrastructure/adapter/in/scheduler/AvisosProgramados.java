package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.scheduler;

import co.edu.ucundinamarca.cundiapp.application.port.in.EntregarAvisosPendientes;
import co.edu.ucundinamarca.cundiapp.application.port.in.GenerarAvisosDeVencimiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * La ronda de avisos (RF10, SCRUM-37): cada pocos minutos crea los avisos de lo que está por vencer y entrega
 * los pendientes de entrega. Una falla no mata la tarea: se anota y la siguiente ronda lo vuelve a intentar.
 *
 * <p>Se apaga con {@code cundiapp.avisos.programados=false}; las pruebas lo hacen para disparar las rondas a
 * mano y no depender de la hora.
 */
@Component
@ConditionalOnProperty(name = "cundiapp.avisos.programados", havingValue = "true", matchIfMissing = true)
class AvisosProgramados {

	private static final Logger log = LoggerFactory.getLogger(AvisosProgramados.class);

	private final GenerarAvisosDeVencimiento generar;
	private final EntregarAvisosPendientes entregar;

	AvisosProgramados(GenerarAvisosDeVencimiento generar, EntregarAvisosPendientes entregar) {
		this.generar = generar;
		this.entregar = entregar;
	}

	@Scheduled(fixedDelayString = "${cundiapp.avisos.cada:PT5M}", initialDelayString = "${cundiapp.avisos.espera-inicial:PT1M}")
	void ronda() {
		try {
			int creados = generar.ejecutar();
			int entregados = entregar.ejecutar();
			if (creados > 0 || entregados > 0) {
				log.info("Ronda de avisos: {} creados, {} entregados", creados, entregados);
			}
		} catch (RuntimeException e) {
			log.error("La ronda de avisos falló; se reintenta en la siguiente", e);
		}
	}
}
