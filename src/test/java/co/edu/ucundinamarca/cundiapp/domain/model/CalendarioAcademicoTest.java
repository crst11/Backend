package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Qué clase de día es una fecha: lectivo, festivo o fuera del período (RF07, SCRUM-85). */
class CalendarioAcademicoTest {

	private final CalendarioAcademico calendario = new CalendarioAcademico();

	@Test
	void unFestivoDentroDelPeriodoEsFestivoYDiceCual() {
		var dia = calendario.clasificar(LocalDate.of(2026, 10, 12));

		assertThat(dia.tipo()).isEqualTo(TipoDeDia.FESTIVO);
		assertThat(dia.festivo()).contains("Día de la Raza");
		assertThat(dia.tieneClase()).isFalse();
	}

	@Test
	void elDiaSiguienteAUnFestivoEsLectivo() {
		var dia = calendario.clasificar(LocalDate.of(2026, 10, 13));

		assertThat(dia.tipo()).isEqualTo(TipoDeDia.LECTIVO);
		assertThat(dia.festivo()).isEmpty();
		assertThat(dia.tieneClase()).isTrue();
	}

	@ParameterizedTest
	@CsvSource({"2026-02-01", "2026-03-10", "2026-06-30", "2026-08-01", "2026-09-15", "2026-12-15"})
	void losDiasDentroDeUnSemestreSonLectivos(String fecha) {
		assertThat(calendario.clasificar(LocalDate.parse(fecha)).tipo()).isEqualTo(TipoDeDia.LECTIVO);
	}

	@ParameterizedTest
	@CsvSource({"2026-01-15", "2026-07-15", "2026-12-16", "2026-12-30"})
	void entreSemestresEstaFueraDelPeriodo(String fecha) {
		var dia = calendario.clasificar(LocalDate.parse(fecha));

		assertThat(dia.tipo()).isEqualTo(TipoDeDia.FUERA_DE_PERIODO);
		assertThat(dia.tieneClase()).isFalse();
	}

	@Test
	void unFestivoFueraDelPeriodoSigueSiendoFestivo() {
		assertThat(calendario.clasificar(LocalDate.of(2026, 12, 25)).tipo()).isEqualTo(TipoDeDia.FESTIVO);
	}

	@Test
	void losFinesDeSemanaDentroDelPeriodoSonLectivosPorqueHayClasesEnSabado() {
		assertThat(calendario.clasificar(LocalDate.of(2026, 10, 17)).tipo()).isEqualTo(TipoDeDia.LECTIVO);
	}

	@Test
	void entreDosFechasDevuelveUnDiaPorFechaEnOrden() {
		var dias = calendario.entre(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 14));

		assertThat(dias).extracting(DiaDelCalendario::fecha).containsExactly(
				LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 14));
		assertThat(dias).extracting(DiaDelCalendario::tipo).containsExactly(
				TipoDeDia.LECTIVO, TipoDeDia.FESTIVO, TipoDeDia.LECTIVO, TipoDeDia.LECTIVO);
	}

	@Test
	void elSiguienteDiaLectivoSaltaFestivosYDiasFueraDelPeriodo() {
		assertThat(calendario.siguienteDiaLectivoDespuesDe(LocalDate.of(2026, 10, 11))).isEqualTo(LocalDate.of(2026, 10, 13));
		assertThat(calendario.siguienteDiaLectivoDespuesDe(LocalDate.of(2026, 12, 15))).isEqualTo(LocalDate.of(2027, 2, 1));
	}
}
