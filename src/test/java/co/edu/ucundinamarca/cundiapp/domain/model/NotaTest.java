package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** La nota de una actividad: de 0.0 a 5.0, con a lo sumo dos decimales (RF05, SCRUM-28). */
class NotaTest {

	@ParameterizedTest
	@ValueSource(strings = {"0", "0.0", "3", "3.5", "4.25", "5", "5.0", "5.00"})
	void aceptaLasNotasDeLaEscala(String texto) {
		assertThat(new Nota(new BigDecimal(texto)).valor()).isEqualByComparingTo(texto);
	}

	@ParameterizedTest
	@ValueSource(strings = {"-0.1", "-1", "5.01", "5.1", "6", "50"})
	void rechazaLoQueSaleDeLaEscala(String texto) {
		assertThatThrownBy(() -> new Nota(new BigDecimal(texto)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("0.0 y 5.0");
	}

	@Test
	void rechazaMasDeDosDecimales() {
		assertThatThrownBy(() -> new Nota(new BigDecimal("4.255")))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("dos decimales");
	}

	@Test
	void unaNotaConCerosDeSobraNoCuentaComoDecimales() {
		// 4.500 vale lo mismo que 4.5: no hay que rechazarla por cómo viene escrita.
		assertThat(new Nota(new BigDecimal("4.500")).valor()).isEqualByComparingTo("4.5");
	}

	@Test
	void sinNotaNoHayNada() {
		assertThatThrownBy(() -> new Nota(null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("nota");
	}
}
