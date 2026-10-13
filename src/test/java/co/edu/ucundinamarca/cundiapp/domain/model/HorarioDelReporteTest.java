package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Las reglas que salen de leer el Reporte de Horario de la universidad (RF04, SCRUM-81). */
class HorarioDelReporteTest {

	@Nested
	class LaHoraDeFin {

		@ParameterizedTest
		@CsvSource({"09:00,10:59,11:00", "07:00,08:59,09:00", "15:00,17:59,18:00", "16:00,17:59,18:00"})
		void laUniversidadEscribeElFinUnMinutoAntesDeLaHoraEnPunto(String inicio, String finDelReporte, String esperado) {
			var bloque = BloqueDeHorario.delReporte(
					1, DiaDeLaSemana.MARTES, LocalTime.parse(inicio), LocalTime.parse(finDelReporte), null, null);

			assertThat(bloque.horaFin()).isEqualTo(LocalTime.parse(esperado));
			assertThat(bloque.horaInicio()).isEqualTo(LocalTime.parse(inicio));
		}

		@Test
		void unaHoraQueNoTerminaEnCincuentaYNueveSeRespeta() {
			var bloque = BloqueDeHorario.delReporte(
					1, DiaDeLaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(9, 30), null, null);

			assertThat(bloque.horaFin()).isEqualTo(LocalTime.of(9, 30));
		}

		@Test
		void lasMedianochesNoSeDesbordan() {
			var bloque = BloqueDeHorario.delReporte(
					1, DiaDeLaSemana.LUNES, LocalTime.of(22, 0), LocalTime.of(23, 59), null, null);

			assertThat(bloque.horaFin()).isEqualTo(LocalTime.of(23, 59));
		}

		@Test
		void siElFinNoEsPosteriorAlInicioSeRechazaComoCualquierBloque() {
			assertThatThrownBy(() -> BloqueDeHorario.delReporte(
					1, DiaDeLaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(9, 59), null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class);
		}
	}

	@Nested
	class ElPeriodo {

		@ParameterizedTest
		@CsvSource({"2026-10-12,2026-2", "2026-08-03,2026-2", "2026-03-15,2026-1", "2026-02-01,2026-1", "2027-01-20,2027-1"})
		void sinFechaDeInicioElReporteDicePorSuFechaACualSemestrePertenece(String fecha, String periodo) {
			assertThat(PeriodoAcademico.delaFecha(LocalDate.parse(fecha)).codigo()).isEqualTo(periodo);
		}

		@Test
		void julioYaEsDelSegundoSemestre() {
			assertThat(PeriodoAcademico.delaFecha(LocalDate.of(2026, 7, 10)).codigo()).isEqualTo("2026-2");
		}
	}

	@Nested
	class LasAsignaturasDelReporte {

		@Test
		void unaAsignaturaLlevaSuCodigoSuGrupoYSusBloques() {
			var bloque = BloqueDeHorario.delReporte(
					1, DiaDeLaSemana.MARTES, LocalTime.of(9, 0), LocalTime.of(10, 59), null, null);

			var asignatura = new AsignaturaConHorario(" CAD612021519 ", " SIS.FUSA 501M ", java.util.List.of(bloque));

			assertThat(asignatura.codigo()).isEqualTo("CAD612021519");
			assertThat(asignatura.grupo()).isEqualTo("SIS.FUSA 501M");
			assertThat(asignatura.bloques()).containsExactly(bloque);
		}

		@Test
		void elGrupoEsOpcionalYUnaAsignaturaNecesitaCodigo() {
			assertThat(new AsignaturaConHorario("CAD1", null, null).grupo()).isNull();
			assertThat(new AsignaturaConHorario("CAD1", " ", null).bloques()).isEmpty();
			assertThatThrownBy(() -> new AsignaturaConHorario(" ", "G", null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class);
		}

		@Test
		void elGrupoRespetaLoQueCabeEnLaBaseDeDatos() {
			assertThatThrownBy(() -> new AsignaturaConHorario("CAD1", "x".repeat(21), null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("20");
		}
	}
}
