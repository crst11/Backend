package co.edu.ucundinamarca.cundiapp.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Cuánto esperar se dice de forma que se lea de un vistazo (SCRUM-88). */
class DemasiadasPeticionesExceptionTest {

	@Test
	void menosDeUnMinutoSeDiceEnSegundos() {
		assertThat(DemasiadasPeticionesException.textoDeEspera(Duration.ofSeconds(45))).isEqualTo("45 segundos");
		assertThat(DemasiadasPeticionesException.textoDeEspera(Duration.ofSeconds(1))).isEqualTo("1 segundo");
	}

	@Test
	void unMinutoOMasSeDiceEnMinutosHaciaArriba() {
		assertThat(DemasiadasPeticionesException.textoDeEspera(Duration.ofSeconds(60))).isEqualTo("1 minuto");
		assertThat(DemasiadasPeticionesException.textoDeEspera(Duration.ofSeconds(61))).isEqualTo("2 minutos");
		assertThat(DemasiadasPeticionesException.textoDeEspera(Duration.ofMinutes(3))).isEqualTo("3 minutos");
	}

	@Test
	void losSegundosSeRedondeanHaciaArribaYNuncaSonCero() {
		assertThat(DemasiadasPeticionesException.segundosDeEspera(Duration.ofMillis(300))).isEqualTo(1);
		assertThat(DemasiadasPeticionesException.segundosDeEspera(Duration.ofMillis(2100))).isEqualTo(3);
		assertThat(DemasiadasPeticionesException.segundosDeEspera(Duration.ZERO)).isEqualTo(1);
	}

	@Test
	void elMensajeDiceCuantoEsperar() {
		assertThat(new DemasiadasPeticionesException(Duration.ofMinutes(3)).getMessage())
				.contains("Espera 3 minutos");
	}
}
