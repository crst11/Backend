package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Los festivos de Colombia salen de la ley, no de un servicio externo (RF07, SCRUM-85). */
class FestivosDeColombiaTest {

	private static List<LocalDate> fechas(int anio) {
		return List.copyOf(FestivosDeColombia.delAnio(anio).keySet());
	}

	@Nested
	class Pascua {

		@ParameterizedTest
		@CsvSource({"2024,2024-03-31", "2025,2025-04-20", "2026,2026-04-05", "2027,2027-03-28", "2028,2028-04-16"})
		void calculaElDomingoDeResurreccion(int anio, String esperado) {
			assertThat(FestivosDeColombia.domingoDePascua(anio)).isEqualTo(LocalDate.parse(esperado));
		}
	}

	@Nested
	class Festivos2026 {

		@Test
		void sonLosDieciochoDelCalendarioOficial() {
			assertThat(fechas(2026)).containsExactly(
					LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 3, 23),
					LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 5, 1),
					LocalDate.of(2026, 5, 18), LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 15),
					LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 7),
					LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2),
					LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25));
		}

		@Test
		void elDoceDeOctubreDeEseAnioEsFestivoYSeLlamaDiaDeLaRaza() {
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2026, 10, 12))).contains("Día de la Raza");
		}
	}

	@Nested
	class Festivos2027 {

		@Test
		void siguenLaMismaLey() {
			assertThat(fechas(2027)).containsExactly(
					LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 11), LocalDate.of(2027, 3, 22),
					LocalDate.of(2027, 3, 25), LocalDate.of(2027, 3, 26), LocalDate.of(2027, 5, 1),
					LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 31), LocalDate.of(2027, 6, 7),
					LocalDate.of(2027, 7, 5), LocalDate.of(2027, 7, 20), LocalDate.of(2027, 8, 7),
					LocalDate.of(2027, 8, 16), LocalDate.of(2027, 10, 18), LocalDate.of(2027, 11, 1),
					LocalDate.of(2027, 11, 15), LocalDate.of(2027, 12, 8), LocalDate.of(2027, 12, 25));
		}
	}

	@Nested
	class LasReglas {

		@Test
		void losQueSeTrasladanCaenSiempreEnLunes() {
			for (int anio = 2024; anio <= 2032; anio++) {
				Map<LocalDate, String> festivos = FestivosDeColombia.delAnio(anio);
				for (var festivo : festivos.entrySet()) {
					boolean fijo = List.of("Año Nuevo", "Día del Trabajo", "Grito de Independencia", "Batalla de Boyacá",
							"Inmaculada Concepción", "Navidad", "Jueves Santo", "Viernes Santo").contains(festivo.getValue());
					if (!fijo) {
						assertThat(festivo.getKey().getDayOfWeek()).as("%s de %d", festivo.getValue(), anio)
								.isEqualTo(DayOfWeek.MONDAY);
					}
				}
			}
		}

		@Test
		void unFestivoQueYaCaeEnLunesNoSeMueve() {
			// El 29 de junio de 2026 y el 20 de julio de 2026 son lunes.
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2026, 6, 29))).isPresent();
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2026, 7, 20))).isPresent();
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2026, 7, 27))).isEmpty();
		}

		@Test
		void losFijosNoSeTrasladanAunqueCaigaEnFinDeSemana() {
			// El 7 de agosto de 2027 es sábado y sigue siendo ese día; el primero de mayo también.
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2027, 8, 7))).contains("Batalla de Boyacá");
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2027, 5, 1))).contains("Día del Trabajo");
		}

		@Test
		void unDiaCualquieraNoEsFestivo() {
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2026, 10, 13))).isEmpty();
			assertThat(FestivosDeColombia.esFestivo(LocalDate.of(2026, 10, 13))).isFalse();
			assertThat(FestivosDeColombia.esFestivo(LocalDate.of(2026, 10, 12))).isTrue();
		}

		@Test
		void haySiempreEntreDiecisieteYDieciochoDiasFestivosPorAnio() {
			for (int anio = 2024; anio <= 2035; anio++) {
				assertThat(FestivosDeColombia.delAnio(anio).size()).as("año %d", anio).isBetween(17, 18);
			}
		}

		@Test
		void siDosFestivosCaenElMismoDiaSeJuntanEnUno() {
			// En 2025 San Pedro y San Pablo (29 de junio, domingo) y el Sagrado Corazón caen el lunes 30.
			assertThat(FestivosDeColombia.nombreDe(LocalDate.of(2025, 6, 30)).orElseThrow())
					.contains("San Pedro y San Pablo").contains("Sagrado Corazón");
			assertThat(FestivosDeColombia.delAnio(2025)).hasSize(17);
		}
	}
}
