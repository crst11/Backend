package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cómo se lee el horario de la semana: por día y en orden de hora (RF04, SCRUM-25). */
class HorarioSemanalTest {

	private static ClaseDelHorario clase(String asignatura, DiaDeLaSemana dia, int desde, int hasta) {
		return new ClaseDelHorario(1, "COD", asignatura,
				new BloqueDeHorario(1, dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0), null, null));
	}

	@Test
	void sinClasesNoHayNingunDiaConClases() {
		var horario = new HorarioSemanal(List.of());

		assertThat(horario.estaVacio()).isTrue();
		assertThat(horario.dias()).isEmpty();
		assertThat(horario.totalDeClases()).isZero();
	}

	@Test
	void agrupaPorDiaDeLunesADomingoSinImportarElOrdenEnQueLleguen() {
		var horario = new HorarioSemanal(List.of(
				clase("Física", DiaDeLaSemana.VIERNES, 8, 10),
				clase("Álgebra", DiaDeLaSemana.LUNES, 10, 12),
				clase("Cálculo", DiaDeLaSemana.MIERCOLES, 8, 10)));

		assertThat(horario.dias()).extracting(DiaConClases::dia)
				.containsExactly(DiaDeLaSemana.LUNES, DiaDeLaSemana.MIERCOLES, DiaDeLaSemana.VIERNES);
		assertThat(horario.totalDeClases()).isEqualTo(3);
	}

	@Test
	void dentroDeUnDiaVanEnOrdenDeHora() {
		var horario = new HorarioSemanal(List.of(
				clase("Tarde", DiaDeLaSemana.MARTES, 14, 16),
				clase("Mañana", DiaDeLaSemana.MARTES, 7, 9),
				clase("Medio día", DiaDeLaSemana.MARTES, 11, 13)));

		assertThat(horario.delDia(DiaDeLaSemana.MARTES)).extracting(ClaseDelHorario::asignatura)
				.containsExactly("Mañana", "Medio día", "Tarde");
	}

	@Test
	void unDiaSinClasesDevuelveUnaListaVaciaYNoFalla() {
		var horario = new HorarioSemanal(List.of(clase("Álgebra", DiaDeLaSemana.LUNES, 8, 10)));

		assertThat(horario.delDia(DiaDeLaSemana.SABADO)).isEmpty();
	}

	@Test
	void dosClasesALaMismaHoraSeMuestranAmbasEnVezDeEsconderUna() {
		var horario = new HorarioSemanal(List.of(
				clase("Álgebra", DiaDeLaSemana.JUEVES, 8, 10),
				clase("Física", DiaDeLaSemana.JUEVES, 8, 10)));

		assertThat(horario.delDia(DiaDeLaSemana.JUEVES)).hasSize(2);
	}
}
