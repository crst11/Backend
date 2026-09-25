package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LimitadorDeIntentosEnMemoriaTest {

	private static final class RelojManual implements RelojPort {
		private Instant ahora = Instant.parse("2026-01-15T10:00:00Z");

		@Override
		public Instant ahora() {
			return ahora;
		}

		void avanzar(Duration tiempo) {
			ahora = ahora.plus(tiempo);
		}
	}

	private final RelojManual reloj = new RelojManual();
	private final LimitadorDeIntentosEnMemoria limitador = new LimitadorDeIntentosEnMemoria(reloj);

	@Test
	void unCorreoSeBloqueaAlQuintoFalloSeguido() {
		for (int i = 0; i < 4; i++) {
			limitador.registrarFallo("correo:ana@ucundinamarca.edu.co");
		}
		assertThat(limitador.estaBloqueado("correo:ana@ucundinamarca.edu.co")).isFalse();

		limitador.registrarFallo("correo:ana@ucundinamarca.edu.co");

		assertThat(limitador.estaBloqueado("correo:ana@ucundinamarca.edu.co")).isTrue();
	}

	@Test
	void unaIpTolera20FallosPorqueVariosEstudiantesComparteRed() {
		for (int i = 0; i < 19; i++) {
			limitador.registrarFallo("ip:10.0.0.1");
		}
		assertThat(limitador.estaBloqueado("ip:10.0.0.1")).isFalse();

		limitador.registrarFallo("ip:10.0.0.1");

		assertThat(limitador.estaBloqueado("ip:10.0.0.1")).isTrue();
	}

	@Test
	void elBloqueoSeLevantaA15MinutosDelUltimoFallo() {
		for (int i = 0; i < 5; i++) {
			limitador.registrarFallo("correo:ana@ucundinamarca.edu.co");
		}
		reloj.avanzar(Duration.ofMinutes(14));
		assertThat(limitador.estaBloqueado("correo:ana@ucundinamarca.edu.co")).isTrue();

		reloj.avanzar(Duration.ofMinutes(2));

		assertThat(limitador.estaBloqueado("correo:ana@ucundinamarca.edu.co")).isFalse();
	}

	@Test
	void unInicioDeSesionCorrectoReiniciaLaCuenta() {
		for (int i = 0; i < 4; i++) {
			limitador.registrarFallo("correo:ana@ucundinamarca.edu.co");
		}

		limitador.reiniciar("correo:ana@ucundinamarca.edu.co");
		limitador.registrarFallo("correo:ana@ucundinamarca.edu.co");

		assertThat(limitador.estaBloqueado("correo:ana@ucundinamarca.edu.co")).isFalse();
	}

	@Test
	void loQueFalloUnCorreoNoBloqueaAOtro() {
		for (int i = 0; i < 5; i++) {
			limitador.registrarFallo("correo:ana@ucundinamarca.edu.co");
		}

		assertThat(limitador.estaBloqueado("correo:luis@ucundinamarca.edu.co")).isFalse();
	}
}
