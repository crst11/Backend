package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VinculoConGoogleTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	@Test
	void reconoceSiEsLaMismaCuentaDeGooglePorSuIdentificador() {
		var vinculo = new VinculoConGoogle("1098765", "ana.diaz@gmail.com", AHORA);

		assertThat(vinculo.esDe("1098765")).isTrue();
		assertThat(vinculo.esDe("otro")).isFalse();
	}

	@Test
	void exigeIdentificadorCorreoYFecha() {
		assertThatThrownBy(() -> new VinculoConGoogle(" ", "ana.diaz@gmail.com", AHORA))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new VinculoConGoogle("1098765", null, AHORA))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new VinculoConGoogle("1098765", "ana.diaz@gmail.com", null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}
}
