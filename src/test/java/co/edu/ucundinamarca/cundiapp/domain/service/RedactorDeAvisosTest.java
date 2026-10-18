package co.edu.ucundinamarca.cundiapp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Lo que dicen los avisos (RF10, SCRUM-37). El miércoles 14 de octubre de 2026 a las 10:00 en Bogotá
 * es el «ahora» de todas las pruebas.
 */
class RedactorDeAvisosTest {

	private static final Instant AHORA = Instant.parse("2026-10-14T15:00:00Z");

	private final RedactorDeAvisos redactor = new RedactorDeAvisos();

	@Test
	void unVencimientoDeHoyDiceLaHoraDeBogota() {
		// 23:59 en Bogotá es la 04:59 UTC del día siguiente.
		var texto = redactor.vencimiento("Taller de redes", Instant.parse("2026-10-15T04:59:00Z"), AHORA);

		assertThat(texto.titulo()).isEqualTo("Vence pronto");
		assertThat(texto.mensaje()).isEqualTo("«Taller de redes» vence hoy a las 11:59 p. m.");
	}

	@Test
	void unRiesgoQueSubeDiceDeQueNivelAQueNivel() {
		var texto = redactor.cambioDeRiesgo("Cálculo", NivelDeRiesgo.MEDIO, NivelDeRiesgo.ALTO);

		assertThat(texto.titulo()).isEqualTo("Subió tu riesgo");
		assertThat(texto.mensaje()).isEqualTo("El riesgo en «Cálculo» pasó de medio a alto.");
	}

	@Test
	void unRiesgoQueBajaTambienLoDice() {
		var texto = redactor.cambioDeRiesgo("Redes", NivelDeRiesgo.ALTO, NivelDeRiesgo.BAJO);

		assertThat(texto.titulo()).isEqualTo("Bajó tu riesgo");
		assertThat(texto.mensaje()).isEqualTo("El riesgo en «Redes» pasó de alto a bajo.");
	}

	@Test
	void unVencimientoDeMananaLoDiceAsi() {
		assertThat(redactor.cuando(Instant.parse("2026-10-15T13:00:00Z"), AHORA)).isEqualTo("mañana a las 8:00 a. m.");
	}

	@Test
	void masLejosDiceElDiaDeLaSemanaYLaFecha() {
		assertThat(redactor.cuando(Instant.parse("2026-10-16T19:30:00Z"), AHORA))
				.isEqualTo("el viernes 16 de octubre a las 2:30 p. m.");
	}

	@Test
	void elMediodiaYLaMedianocheSeEscribenComoDoce() {
		assertThat(redactor.cuando(Instant.parse("2026-10-14T17:00:00Z"), AHORA)).isEqualTo("hoy a las 12:00 p. m.");
		// 00:30 en Bogotá del jueves es la 05:30 UTC; desde el miércoles a las 10:00 es «mañana».
		assertThat(redactor.cuando(Instant.parse("2026-10-15T05:30:00Z"), AHORA)).isEqualTo("mañana a las 12:30 a. m.");
	}

	@Test
	void elDiaSeCuentaEnBogotaYNoEnUtc() {
		// A las 21:00 en Bogotá (02:00 UTC del jueves) todavía es miércoles: lo que vence a las 22:00 es «hoy».
		Instant nocheDelMiercoles = Instant.parse("2026-10-15T02:00:00Z");

		assertThat(redactor.cuando(Instant.parse("2026-10-15T03:00:00Z"), nocheDelMiercoles)).isEqualTo("hoy a las 10:00 p. m.");
	}
}
