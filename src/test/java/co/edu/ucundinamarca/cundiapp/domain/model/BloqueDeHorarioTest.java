package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas de un bloque del horario: el día, las horas, el salón y el docente (RF04, SCRUM-25). */
class BloqueDeHorarioTest {

	private static final LocalTime OCHO = LocalTime.of(8, 0);
	private static final LocalTime DIEZ = LocalTime.of(10, 0);

	@Nested
	class ElDia {

		@ParameterizedTest
		@ValueSource(strings = {"lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"})
		void conoceLosSieteDiasComoLosGuardaLaBaseDeDatos(String valor) {
			assertThat(DiaDeLaSemana.desdeBd(valor).valorEnBd()).isEqualTo(valor);
		}

		@Test
		void unDiaQueNoExisteSeRechazaConUnMensajeUtil() {
			assertThatThrownBy(() -> DiaDeLaSemana.desdeBd("lunas"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("día");
		}

		@Test
		void sabeTraducirseDeLaFechaDeJava() {
			assertThat(DiaDeLaSemana.de(DayOfWeek.WEDNESDAY)).isEqualTo(DiaDeLaSemana.MIERCOLES);
			assertThat(DiaDeLaSemana.de(DayOfWeek.SUNDAY)).isEqualTo(DiaDeLaSemana.DOMINGO);
		}

		@Test
		void seOrdenanDeLunesADomingo() {
			assertThat(DiaDeLaSemana.values()).containsExactly(DiaDeLaSemana.LUNES, DiaDeLaSemana.MARTES,
					DiaDeLaSemana.MIERCOLES, DiaDeLaSemana.JUEVES, DiaDeLaSemana.VIERNES, DiaDeLaSemana.SABADO,
					DiaDeLaSemana.DOMINGO);
		}
	}

	@Nested
	class LasHoras {

		@Test
		void empiezaAntesDeTerminar() {
			var bloque = new BloqueDeHorario(1, DiaDeLaSemana.LUNES, OCHO, DIEZ, null, null);

			assertThat(bloque.horaInicio()).isEqualTo(OCHO);
			assertThat(bloque.duracionEnMinutos()).isEqualTo(120);
		}

		@Test
		void unaClaseQueTerminaAntesDeEmpezarSeRechazaDiciendoLasHoras() {
			assertThatThrownBy(() -> new BloqueDeHorario(1, DiaDeLaSemana.LUNES, DIEZ, OCHO, null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("antes");
		}

		@Test
		void unaClaseQueEmpiezaYTerminaALaMismaHoraTampocoExiste() {
			assertThatThrownBy(() -> new BloqueDeHorario(1, DiaDeLaSemana.LUNES, OCHO, OCHO, null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class);
		}

		@Test
		void losSegundosNoCuentanEnUnHorarioDeClases() {
			var bloque = new BloqueDeHorario(1, DiaDeLaSemana.LUNES, LocalTime.of(8, 0, 30), DIEZ, null, null);

			assertThat(bloque.horaInicio()).isEqualTo(OCHO);
		}

		@Test
		void faltaElDiaOLasHoras() {
			assertThatThrownBy(() -> new BloqueDeHorario(1, null, OCHO, DIEZ, null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("día");
			assertThatThrownBy(() -> new BloqueDeHorario(1, DiaDeLaSemana.LUNES, null, DIEZ, null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("hora");
			assertThatThrownBy(() -> new BloqueDeHorario(1, DiaDeLaSemana.LUNES, OCHO, null, null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("hora");
		}

		@Test
		void elNumeroDeOrdenEmpiezaEnUno() {
			assertThatThrownBy(() -> new BloqueDeHorario(0, DiaDeLaSemana.LUNES, OCHO, DIEZ, null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class);
		}
	}

	@Nested
	class ElEspacio {

		@Test
		void guardaSalonBloqueYSede() {
			var espacio = new EspacioFisico("F-203", "Bloque F", "Fusagasugá");

			assertThat(espacio.salon()).isEqualTo("F-203");
			assertThat(espacio.bloque()).isEqualTo("Bloque F");
			assertThat(espacio.sede()).isEqualTo("Fusagasugá");
		}

		@Test
		void elBloqueEsOpcionalPeroElSalonYLaSedeNo() {
			assertThat(new EspacioFisico("Auditorio", null, "Fusagasugá").bloque()).isNull();
			assertThatThrownBy(() -> new EspacioFisico(" ", "F", "Fusagasugá"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("salón");
			assertThatThrownBy(() -> new EspacioFisico("F-203", "F", ""))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("sede");
		}

		@Test
		void recortaLosEspaciosYUnBloqueEnBlancoEsNoTenerBloque() {
			var espacio = new EspacioFisico("  F-203 ", "   ", " Fusagasugá ");

			assertThat(espacio.salon()).isEqualTo("F-203");
			assertThat(espacio.bloque()).isNull();
			assertThat(espacio.sede()).isEqualTo("Fusagasugá");
		}

		@Test
		void respetaLoQueCabeEnLaBaseDeDatos() {
			assertThatThrownBy(() -> new EspacioFisico("x".repeat(31), null, "Fusagasugá"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("30");
			assertThatThrownBy(() -> new EspacioFisico("F-203", null, "x".repeat(61)))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("60");
		}

		@Test
		void elBloqueAdmiteElNombreLargoQueTraeElReporteDeHorario() {
			var largo = "BLOQUE D- ACADEMICO Y ADMINISTRATIVO";

			assertThat(new EspacioFisico("D-101", largo, "Fusagasugá").bloque()).isEqualTo(largo);
			assertThatThrownBy(() -> new EspacioFisico("D-101", "x".repeat(61), "Fusagasugá"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("60");
		}
	}

	@Nested
	class ElDocente {

		@Test
		void seIdentificaPorSuNombreSinTildesNiMayusculasNiEspaciosDeMas() {
			var uno = new Docente("María  José", "Pérez   Gómez");
			var otro = new Docente("maria jose", "PEREZ GOMEZ");

			assertThat(uno.nombreNormalizado()).isEqualTo("maria jose perez gomez");
			assertThat(otro.nombreNormalizado()).isEqualTo(uno.nombreNormalizado());
		}

		@Test
		void necesitaNombresYApellidos() {
			assertThatThrownBy(() -> new Docente("", "Pérez")).isInstanceOf(ReglaDeNegocioVioladaException.class);
			assertThatThrownBy(() -> new Docente("María", null)).isInstanceOf(ReglaDeNegocioVioladaException.class);
		}

		@Test
		void respetaLoQueCabeEnLaBaseDeDatos() {
			assertThatThrownBy(() -> new Docente("x".repeat(81), "Pérez"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("80");
		}

		@Test
		void elBloqueLoLleva() {
			var bloque = new BloqueDeHorario(1, DiaDeLaSemana.MARTES, OCHO, DIEZ,
					new EspacioFisico("F-203", "F", "Fusagasugá"), new Docente("María", "Pérez"));

			assertThat(bloque.espacio()).isPresent();
			assertThat(bloque.docente()).isPresent();
			assertThat(new BloqueDeHorario(1, DiaDeLaSemana.MARTES, OCHO, DIEZ, null, null).espacio()).isEmpty();
		}
	}
}
