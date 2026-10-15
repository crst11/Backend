package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Lo que le toca al estudiante un día: sus clases si ese día hay clase, y si no, cuál es el próximo día
 * que sí (RF07, SCRUM-31 y SCRUM-32). Las fechas son las de octubre de 2026: el lunes 12 es festivo.
 */
class AgendaDelDiaTest {

	private static final LocalDate LUNES_FESTIVO = LocalDate.of(2026, 10, 12);
	private static final LocalDate MARTES = LocalDate.of(2026, 10, 13);
	private static final LocalDate MIERCOLES = LocalDate.of(2026, 10, 14);

	private final CalendarioAcademico calendario = new CalendarioAcademico();

	private static ClaseDelHorario clase(int id, String nombre, DiaDeLaSemana dia, int desde, int hasta) {
		return new ClaseDelHorario(id, "CAD" + id, nombre, new BloqueDeHorario(
				1, dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), new EspacioFisico("E-302", null, "Fusagasugá"), null));
	}

	private final HorarioSemanal horario = new HorarioSemanal(List.of(
			clase(1, "Ecuaciones", DiaDeLaSemana.MARTES, 9, 11),
			clase(2, "Sistemas", DiaDeLaSemana.MARTES, 15, 18),
			clase(3, "Software", DiaDeLaSemana.MARTES, 7, 9),
			clase(4, "Datos", DiaDeLaSemana.MIERCOLES, 9, 11),
			clase(5, "Lengua", DiaDeLaSemana.LUNES, 7, 9)));

	private AgendaDelDia agenda(LocalDate fecha, LocalDateTime ahora) {
		return AgendaDelDia.de(fecha, ahora, horario, calendario);
	}

	@Nested
	class UnDiaConClase {

		@Test
		void muestraLasClasesDeEseDiaEnOrdenDeHora() {
			var agenda = agenda(MARTES, MARTES.atTime(6, 0));

			assertThat(agenda.dia().tipo()).isEqualTo(TipoDeDia.LECTIVO);
			assertThat(agenda.clases()).extracting(c -> c.clase().asignatura())
					.containsExactly("Software", "Ecuaciones", "Sistemas");
		}

		@Test
		void antesDeEmpezarLaPrimeraEsLaProximaYLasDemasVienenDespues() {
			var agenda = agenda(MARTES, MARTES.atTime(6, 0));

			assertThat(agenda.clases()).extracting(ClaseDeLaAgenda::estado)
					.containsExactly(EstadoDeLaClase.PROXIMA, EstadoDeLaClase.LUEGO, EstadoDeLaClase.LUEGO);
		}

		@Test
		void mientrasUnaClaseSeDictaEstaEnCursoYLaSiguienteEsLaProxima() {
			var agenda = agenda(MARTES, MARTES.atTime(9, 30));

			assertThat(agenda.clases()).extracting(ClaseDeLaAgenda::estado)
					.containsExactly(EstadoDeLaClase.PASADA, EstadoDeLaClase.EN_CURSO, EstadoDeLaClase.PROXIMA);
		}

		@Test
		void entreDosClasesLaQueYaTerminoQuedaPasadaYLaSiguienteEsLaProxima() {
			var agenda = agenda(MARTES, MARTES.atTime(12, 0));

			assertThat(agenda.clases()).extracting(ClaseDeLaAgenda::estado)
					.containsExactly(EstadoDeLaClase.PASADA, EstadoDeLaClase.PASADA, EstadoDeLaClase.PROXIMA);
		}

		@Test
		void alTerminarLaUltimaTodasQuedanPasadasYNoHayProxima() {
			var agenda = agenda(MARTES, MARTES.atTime(19, 0));

			assertThat(agenda.clases()).extracting(ClaseDeLaAgenda::estado)
					.containsOnly(EstadoDeLaClase.PASADA);
		}

		@Test
		void laHoraDeFinNoCuentaComoEnCurso() {
			var agenda = agenda(MARTES, MARTES.atTime(11, 0));

			assertThat(agenda.clases().get(1).estado()).isEqualTo(EstadoDeLaClase.PASADA);
		}

		@Test
		void unDiaFuturoTieneTodasSusClasesPorVenir() {
			var agenda = agenda(MIERCOLES, MARTES.atTime(10, 0));

			assertThat(agenda.clases()).extracting(ClaseDeLaAgenda::estado).containsOnly(EstadoDeLaClase.LUEGO);
		}

		@Test
		void unDiaPasadoTieneTodasSusClasesPasadas() {
			var agenda = agenda(MARTES, MIERCOLES.atTime(10, 0));

			assertThat(agenda.clases()).extracting(ClaseDeLaAgenda::estado).containsOnly(EstadoDeLaClase.PASADA);
		}
	}

	@Nested
	class UnDiaSinClase {

		@Test
		void unFestivoNoMuestraClasesAunqueElHorarioTengaClaseEseDiaDeLaSemana() {
			var agenda = agenda(LUNES_FESTIVO, LUNES_FESTIVO.atTime(6, 0));

			assertThat(agenda.dia().tipo()).isEqualTo(TipoDeDia.FESTIVO);
			assertThat(agenda.dia().festivo()).contains("Día de la Raza");
			assertThat(agenda.clases()).isEmpty();
		}

		@Test
		void elFestivoApuntaAlProximoDiaConClases() {
			var agenda = agenda(LUNES_FESTIVO, LUNES_FESTIVO.atTime(6, 0));

			assertThat(agenda.siguiente()).isPresent();
			assertThat(agenda.siguiente().orElseThrow().dia().fecha()).isEqualTo(MARTES);
			assertThat(agenda.siguiente().orElseThrow().clases()).hasSize(3);
		}

		@Test
		void unDiaLectivoSinClasesEnElHorarioTambienApuntaAlSiguiente() {
			// El jueves no hay ninguna clase en este horario.
			var agenda = agenda(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15).atTime(8, 0));

			assertThat(agenda.dia().tipo()).isEqualTo(TipoDeDia.LECTIVO);
			assertThat(agenda.clases()).isEmpty();
			assertThat(agenda.siguiente().orElseThrow().dia().fecha()).isEqualTo(LocalDate.of(2026, 10, 19));
		}

		@Test
		void fueraDelPeriodoElSiguienteEsElPrimerDiaConClaseDelSemestreQueViene() {
			var agenda = agenda(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 20).atTime(8, 0));

			assertThat(agenda.dia().tipo()).isEqualTo(TipoDeDia.FUERA_DE_PERIODO);
			assertThat(agenda.clases()).isEmpty();
			// El 1 de febrero de 2027 es lunes, el primer día del semestre, y el horario tiene clase de Lengua los lunes.
			assertThat(agenda.siguiente().orElseThrow().dia().fecha()).isEqualTo(LocalDate.of(2027, 2, 1));
		}

		@Test
		void sinNingunaClaseEnElHorarioNoHaySiguiente() {
			var vacio = AgendaDelDia.de(MARTES, MARTES.atTime(8, 0), new HorarioSemanal(List.of()), calendario);

			assertThat(vacio.clases()).isEmpty();
			assertThat(vacio.siguiente()).isEmpty();
		}
	}

	@Test
	void unDiaConClasesTambienDiceCualEsElSiguienteParaMostrarMananaDebajo() {
		var agenda = agenda(MARTES, MARTES.atTime(8, 0));

		assertThat(agenda.siguiente().orElseThrow().dia().fecha()).isEqualTo(MIERCOLES);
		assertThat(agenda.siguiente().orElseThrow().clases()).extracting(ClaseDelHorario::asignatura).containsExactly("Datos");
	}
}
