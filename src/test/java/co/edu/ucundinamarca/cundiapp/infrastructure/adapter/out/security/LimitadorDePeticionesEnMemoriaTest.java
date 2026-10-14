package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** El límite de peticiones cuenta todas, salgan bien o mal, y se libera con el tiempo (SCRUM-88). */
class LimitadorDePeticionesEnMemoriaTest {

	private static final Duration VENTANA = Duration.ofMinutes(10);

	private static final class RelojManual implements RelojPort {
		private Instant ahora = Instant.parse("2026-10-14T10:00:00Z");

		@Override
		public Instant ahora() {
			return ahora;
		}

		void avanzar(Duration tiempo) {
			ahora = ahora.plus(tiempo);
		}
	}

	private final RelojManual reloj = new RelojManual();
	private final LimitadorDePeticionesEnMemoria limitador = new LimitadorDePeticionesEnMemoria(reloj);

	@Test
	void dejaPasarHastaElMaximoYRechazaLaSiguiente() {
		for (int i = 0; i < 3; i++) {
			assertThat(limitador.esperaSiSeExcede("a", 3, VENTANA)).isEmpty();
		}

		assertThat(limitador.esperaSiSeExcede("a", 3, VENTANA)).isPresent();
	}

	@Test
	void diceCuantoFaltaParaQueSeLibere() {
		for (int i = 0; i < 3; i++) {
			limitador.esperaSiSeExcede("a", 3, VENTANA);
		}
		reloj.avanzar(Duration.ofMinutes(4));

		assertThat(limitador.esperaSiSeExcede("a", 3, VENTANA)).contains(Duration.ofMinutes(6));
	}

	@Test
	void pasadaLaVentanaElContadorVuelveACero() {
		for (int i = 0; i < 4; i++) {
			limitador.esperaSiSeExcede("a", 3, VENTANA);
		}
		reloj.avanzar(VENTANA);

		assertThat(limitador.esperaSiSeExcede("a", 3, VENTANA)).isEmpty();
	}

	@Test
	void cadaClaveTieneSuPropioCupo() {
		for (int i = 0; i < 4; i++) {
			limitador.esperaSiSeExcede("ana", 3, VENTANA);
		}

		assertThat(limitador.esperaSiSeExcede("ana", 3, VENTANA)).isPresent();
		assertThat(limitador.esperaSiSeExcede("luis", 3, VENTANA)).isEmpty();
	}
}
