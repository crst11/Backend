package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** El nombre con el que la persona reconoce su dispositivo en la lista (RF10, SCRUM-37). */
class NombreDeDispositivoTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36|Chrome en Android",
			"Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1|Safari en iOS",
			"Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/126.0 Mobile/15E148 Safari/604.1|Chrome en iOS",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0|Edge en Windows",
			"Mozilla/5.0 (X11; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0|Firefox en Linux",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36|Chrome en macOS",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 OPR/111.0.0.0|Opera en Windows",
			"curl/8.5.0|Navegador"
	})
	void reconoceElNavegadorYElSistema(String userAgent, String esperado) {
		assertThat(NombreDeDispositivo.desde(userAgent)).isEqualTo(esperado);
	}

	@Test
	void sinUserAgentUsaEsteDispositivo() {
		assertThat(NombreDeDispositivo.desde(null)).isEqualTo("Este dispositivo");
		assertThat(NombreDeDispositivo.desde("  ")).isEqualTo("Este dispositivo");
	}
}
