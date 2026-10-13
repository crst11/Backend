package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeLaClase;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeDia;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** La pantalla del día con la hora de Bogotá, no la del servidor (SCRUM-31 y SCRUM-32). */
@ExtendWith(MockitoExtension.class)
class ConsultarMiDiaServicioTest {

	private static final int ESTUDIANTE = 5;
	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

	@Mock
	private HorarioRepositorio horarios;
	@Mock
	private EstructuraDeEvaluacionRepositorio evaluaciones;

	private static Clock a(String instante) {
		return Clock.fixed(Instant.parse(instante), BOGOTA);
	}

	private void conUnaClaseElMartesYOtraElMiercoles() {
		given(horarios.clasesDe(ESTUDIANTE)).willReturn(List.of(
				new ClaseDelHorario(7, "CAD1", "Ecuaciones", new BloqueDeHorario(
						1, DiaDeLaSemana.MARTES, LocalTime.of(9, 0), LocalTime.of(11, 0), null, null)),
				new ClaseDelHorario(8, "CAD2", "Sistemas", new BloqueDeHorario(
						1, DiaDeLaSemana.MIERCOLES, LocalTime.of(7, 0), LocalTime.of(9, 0), null, null))));
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(List.of(
				new AsignaturaMatriculada(7, "CAD1", "Ecuaciones", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 3, 12),
				new AsignaturaMatriculada(8, "CAD2", "Sistemas", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 0, 0)));
	}

	@Test
	void sinFechaUsaElDiaDeHoyEnBogotaYLaHoraDeAhora() {
		conUnaClaseElMartesYOtraElMiercoles();
		// 14:30 UTC del martes 13 son las 09:30 en Bogotá: la clase de Ecuaciones se está dictando.
		var servicio = new ConsultarMiDiaServicio(horarios, evaluaciones, a("2026-10-13T14:30:00Z"));

		var resultado = servicio.ejecutar(ESTUDIANTE, null);

		assertThat(resultado.agenda().dia().fecha()).isEqualTo(LocalDate.of(2026, 10, 13));
		assertThat(resultado.agenda().clases().getFirst().estado()).isEqualTo(EstadoDeLaClase.EN_CURSO);
		assertThat(resultado.hoy()).isEqualTo(LocalDate.of(2026, 10, 13));
	}

	@Test
	void pasadaLaMedianocheDeBogotaYaEsElDiaSiguiente() {
		conUnaClaseElMartesYOtraElMiercoles();
		// 05:30 UTC del miércoles son las 00:30 del miércoles en Bogotá.
		var servicio = new ConsultarMiDiaServicio(horarios, evaluaciones, a("2026-10-14T05:30:00Z"));

		assertThat(servicio.ejecutar(ESTUDIANTE, null).agenda().dia().fecha()).isEqualTo(LocalDate.of(2026, 10, 14));
	}

	@Test
	void unFestivoMuestraElProximoDiaConClase() {
		conUnaClaseElMartesYOtraElMiercoles();
		var servicio = new ConsultarMiDiaServicio(horarios, evaluaciones, a("2026-10-12T15:00:00Z"));

		var resultado = servicio.ejecutar(ESTUDIANTE, null);

		assertThat(resultado.agenda().dia().tipo()).isEqualTo(TipoDeDia.FESTIVO);
		assertThat(resultado.agenda().clases()).isEmpty();
		assertThat(resultado.agenda().siguiente().orElseThrow().dia().fecha()).isEqualTo(LocalDate.of(2026, 10, 13));
	}

	@Test
	void diceCuantasActividadesTieneCadaAsignaturaParaOfrecerLasOSubirlas() {
		conUnaClaseElMartesYOtraElMiercoles();
		var servicio = new ConsultarMiDiaServicio(horarios, evaluaciones, a("2026-10-13T11:00:00Z"));

		var resultado = servicio.ejecutar(ESTUDIANTE, null);

		assertThat(resultado.actividadesDe(7)).isEqualTo(12);
		assertThat(resultado.actividadesDe(8)).isZero();
		assertThat(resultado.actividadesDe(999)).isZero();
	}

	@Test
	void conUnaFechaPedidaMuestraEsaFechaPeroHoySigueSiendoHoy() {
		conUnaClaseElMartesYOtraElMiercoles();
		var servicio = new ConsultarMiDiaServicio(horarios, evaluaciones, a("2026-10-13T11:00:00Z"));

		var resultado = servicio.ejecutar(ESTUDIANTE, LocalDate.of(2026, 10, 14));

		assertThat(resultado.agenda().dia().fecha()).isEqualTo(LocalDate.of(2026, 10, 14));
		assertThat(resultado.hoy()).isEqualTo(LocalDate.of(2026, 10, 13));
		assertThat(resultado.agenda().clases().getFirst().estado()).isEqualTo(EstadoDeLaClase.LUEGO);
	}
}
