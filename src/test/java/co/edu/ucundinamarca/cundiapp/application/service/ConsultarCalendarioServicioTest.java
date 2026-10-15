package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeDia;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** El calendario se calcula con la fecha de Bogotá, no con la del servidor (SCRUM-85). */
class ConsultarCalendarioServicioTest {

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

	private static Clock a(String instante) {
		return Clock.fixed(Instant.parse(instante), BOGOTA);
	}

	@Test
	void hoyEsLaFechaDeBogotaAunqueEnUtcYaSeaOtroDia() {
		// 02:00 UTC del 13 de octubre son las 21:00 del 12 en Bogotá, y el 12 es festivo.
		var servicio = new ConsultarCalendarioServicio(a("2026-10-13T02:00:00Z"));

		var calendario = servicio.ejecutar(null, null);

		assertThat(calendario.hoy()).isEqualTo(LocalDate.of(2026, 10, 12));
		assertThat(calendario.dias().getFirst().tipo()).isEqualTo(TipoDeDia.FESTIVO);
	}

	@Test
	void sinRangoTraeHoyYLosSeisDiasSiguientes() {
		var calendario = new ConsultarCalendarioServicio(a("2026-10-12T15:00:00Z")).ejecutar(null, null);

		assertThat(calendario.dias()).hasSize(7);
		assertThat(calendario.dias().getLast().fecha()).isEqualTo(LocalDate.of(2026, 10, 18));
	}

	@Test
	void conRangoTraeEseRango() {
		var calendario = new ConsultarCalendarioServicio(a("2026-10-12T15:00:00Z"))
				.ejecutar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));

		assertThat(calendario.dias()).hasSize(3);
	}

	@Test
	void unRangoAlReves() {
		assertThatThrownBy(() -> new ConsultarCalendarioServicio(a("2026-10-12T15:00:00Z"))
				.ejecutar(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("desde");
	}

	@Test
	void unRangoDemasiadoLargoSeRechazaParaNoArmarRespuestasEnormes() {
		assertThatThrownBy(() -> new ConsultarCalendarioServicio(a("2026-10-12T15:00:00Z"))
				.ejecutar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("92");
	}

	@Test
	void conSoloUnExtremoElOtroSeCompleta() {
		var calendario = new ConsultarCalendarioServicio(a("2026-10-12T15:00:00Z"))
				.ejecutar(LocalDate.of(2026, 10, 14), null);

		assertThat(calendario.dias().getFirst().fecha()).isEqualTo(LocalDate.of(2026, 10, 14));
		assertThat(calendario.dias()).hasSize(7);
	}
}
