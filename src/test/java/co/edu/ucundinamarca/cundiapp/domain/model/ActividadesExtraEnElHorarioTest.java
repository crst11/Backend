package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Cómo se comporta una actividad extra dentro del horario (SCRUM-266): se ordena con las clases, tiene su estado
 * del día, y no es de ninguna asignatura ni de ninguna sede. El martes 13 de octubre de 2026 es lectivo.
 */
class ActividadesExtraEnElHorarioTest {

	private static final LocalDate MARTES = LocalDate.of(2026, 10, 13);

	private final CalendarioAcademico calendario = new CalendarioAcademico();

	private static ClaseDelHorario clase(int id, String nombre, int desde, int hasta) {
		return new ClaseDelHorario(id, "CAD" + id, nombre, new BloqueDeHorario(1, DiaDeLaSemana.MARTES,
				LocalTime.of(desde, 0), LocalTime.of(hasta, 0), new EspacioFisico("E-302", null, "Fusagasugá"), null));
	}

	private static ClaseDelHorario baile() {
		var actividad = new ActividadExtra(5, 7, "Baile", DiaDeLaSemana.MARTES, LocalTime.of(17, 0), LocalTime.of(19, 0),
				"Coliseo", "Profe Ana");
		return ClaseDelHorario.deActividadExtra(actividad);
	}

	private final HorarioSemanal horario = new HorarioSemanal(List.of(
			clase(1, "Ecuaciones", 9, 11),
			clase(2, "Sistemas", 15, 18),
			baile()));

	@Test
	void unaActividadExtraNoEsDeNingunaAsignatura() {
		var extra = baile();

		assertThat(extra.esExtra()).isTrue();
		assertThat(extra.idMatricula()).isNull();
		assertThat(extra.codigoAsignatura()).isNull();
		assertThat(extra.asignatura()).isEqualTo("Baile");
		assertThat(extra.extra().id()).isEqualTo(5);
		assertThat(extra.extra().lugar()).isEqualTo("Coliseo");
		assertThat(extra.bloque().duracionEnMinutos()).isEqualTo(120);
	}

	@Test
	void unaClaseNormalNoEsExtra() {
		var normal = clase(1, "Ecuaciones", 9, 11);

		assertThat(normal.esExtra()).isFalse();
		assertThat(normal.extra()).isNull();
		assertThat(normal.idMatricula()).isEqualTo(1);
	}

	@Test
	void unaClaseNecesitaUnaMatriculaOEsActividadExtraPeroNoLasDosNiNinguna() {
		var bloque = baile().bloque();
		var actividad = baile().extra();

		assertThatThrownBy(() -> new ClaseDelHorario(null, "X", "Y", bloque, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ClaseDelHorario(1, "X", "Y", bloque, actividad))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void seOrdenaConLasClasesPorHora() {
		assertThat(horario.delDia(DiaDeLaSemana.MARTES)).extracting(ClaseDelHorario::asignatura)
				.containsExactly("Ecuaciones", "Sistemas", "Baile");
		assertThat(horario.totalDeClases()).isEqualTo(3);
	}

	@Test
	void noApareceEntreLasAsignaturasNiLasSedesParaElegir() {
		assertThat(horario.asignaturas()).extracting(AsignaturaDelHorario::nombre)
				.containsExactly("Ecuaciones", "Sistemas");
		assertThat(horario.sedes()).containsExactly("Fusagasugá");
	}

	@Test
	void losFiltrosPorAsignaturaYPorSedeLaOcultan() {
		assertThat(horario.filtrar(new FiltroDeHorario("CAD1", null)).clases())
				.extracting(ClaseDelHorario::asignatura).containsExactly("Ecuaciones");
		assertThat(horario.filtrar(new FiltroDeHorario(null, "Fusagasugá")).clases())
				.extracting(ClaseDelHorario::asignatura).containsExactlyInAnyOrder("Ecuaciones", "Sistemas");
		assertThat(horario.filtrar(FiltroDeHorario.SIN_FILTRO).clases()).hasSize(3);
	}

	@Test
	void enLaAgendaDelDiaTieneSuEstadoSegunLaHora() {
		var enCurso = AgendaDelDia.de(MARTES, MARTES.atTime(17, 30), horario, calendario);
		assertThat(enCurso.clases()).extracting(c -> c.clase().asignatura(), ClaseDeLaAgenda::estado)
				.containsExactly(
						tuple("Ecuaciones", EstadoDeLaClase.PASADA),
						tuple("Sistemas", EstadoDeLaClase.EN_CURSO),
						tuple("Baile", EstadoDeLaClase.EN_CURSO));

		var terminadas = AgendaDelDia.de(MARTES, MARTES.atTime(19, 30), horario, calendario);
		assertThat(terminadas.clases()).extracting(ClaseDeLaAgenda::estado)
				.containsExactly(EstadoDeLaClase.PASADA, EstadoDeLaClase.PASADA, EstadoDeLaClase.PASADA);
	}

	@Test
	void unFestivoNoMuestraLaActividadIgualQueNoMuestraLasClases() {
		var festivo = LocalDate.of(2026, 10, 12);
		var lunes = new HorarioSemanal(List.of(ClaseDelHorario.deActividadExtra(
				new ActividadExtra(6, 7, "Fútbol", DiaDeLaSemana.LUNES, LocalTime.of(16, 0), LocalTime.of(18, 0), null, null))));

		assertThat(AgendaDelDia.de(festivo, festivo.atTime(8, 0), lunes, calendario).clases()).isEmpty();
	}
}
