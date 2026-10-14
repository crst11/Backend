package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Peticiones contadas en memoria con una ventana fija por clave (SCRUM-88): la primera abre la ventana
 * y, pasado el tiempo, el contador vuelve a cero. Basta para un solo backend, igual que
 * {@link LimitadorDeIntentosEnMemoria}; con varias instancias habría que llevarlo a un almacén compartido.
 */
@Component
class LimitadorDePeticionesEnMemoria implements LimitadorDePeticionesPort {

	private static final int TAMANO_PARA_LIMPIAR = 10_000;

	private record Ventana(int peticiones, Instant fin) {
	}

	private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
	private final RelojPort reloj;

	LimitadorDePeticionesEnMemoria(RelojPort reloj) {
		this.reloj = reloj;
	}

	@Override
	public Optional<Duration> esperaSiSeExcede(String clave, int maximo, Duration ventana) {
		Instant ahora = reloj.ahora();
		Ventana actual = ventanas.compute(clave, (k, previa) ->
				previa == null || !ahora.isBefore(previa.fin())
						? new Ventana(1, ahora.plus(ventana))
						: new Ventana(previa.peticiones() + 1, previa.fin()));
		if (ventanas.size() > TAMANO_PARA_LIMPIAR) {
			ventanas.values().removeIf(v -> !ahora.isBefore(v.fin()));
		}
		return actual.peticiones() > maximo ? Optional.of(Duration.between(ahora, actual.fin())) : Optional.empty();
	}
}
