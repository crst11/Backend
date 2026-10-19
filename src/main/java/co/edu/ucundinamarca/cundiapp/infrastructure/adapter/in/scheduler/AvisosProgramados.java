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
 * los pendientes de entrega. Una falla no mata la tarea: se anota y la siguiente ronda lo vuelve a intentar. Con
 * varias instancias del backend la ronda la reparte una sola a la vez ({@link UnaSolaInstancia}, SCRUM-220).
 *
 * <p>Se apaga con {@code cundiapp.avisos.programados=false}; las pruebas lo hacen para disparar las rondas a
 * mano y no depender de la hora.
 */
@Component
@ConditionalOnProperty(name = "cundiapp.avisos.programados", havingValue = "true", matchIfMissing = true)
class AvisosProgramados {

	private static final Logger log = LoggerFactory.getLogger(AvisosProgramados.class);

	/** Llave del candado de la ronda en PostgreSQL: la misma en todas las instancias. */
	static final long LLAVE_DE_LA_RONDA = 3_700_001L;

	private final GenerarAvisosDeVencimiento generar;
	private final EntregarAvisosPendientes entregar;
	private final UnaSolaInstancia unaSolaInstancia;

	AvisosProgramados(GenerarAvisosDeVencimiento generar, EntregarAvisosPendientes entregar, UnaSolaInstancia unaSolaInstancia) {
		this.generar = generar;
		this.entregar = entregar;
		this.unaSolaInstancia = unaSolaInstancia;
	}

	@Scheduled(fixedDelayString = "${cundiapp.avisos.cada:PT5M}", initialDelayString = "${cundiapp.avisos.espera-inicial:PT1M}")
	void ronda() {
		try {
			// Con varias instancias solo una reparte la ronda; si no, el mismo aviso saldría por push más de una vez.
			if (!unaSolaInstancia.ejecutar(LLAVE_DE_LA_RONDA, this::repartir)) {
				log.debug("Otra instancia está repartiendo la ronda de avisos; esta la deja pasar");
			}
		} catch (RuntimeException e) {
			log.error("La ronda de avisos falló; se reintenta en la siguiente", e);
		}
	}

	private void repartir() {
		int creados = generar.ejecutar();
		int entregados = entregar.ejecutar();
		if (creados > 0 || entregados > 0) {
			log.info("Ronda de avisos: {} creados, {} entregados", creados, entregados);
		}
	}
}
