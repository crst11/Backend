package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadExtra;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeImportacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** El avance sale de lo que ya existe: las importaciones, el horario y las actividades (SCRUM-84). */
@ExtendWith(MockitoExtension.class)
class ConsultarMiAvanceDeInformacionServicioTest {

	private static final int ESTUDIANTE = 5;

	@Mock
	private ImportacionRepositorio importaciones;
	@Mock
	private HorarioRepositorio horarios;
	@Mock
	private EstructuraDeEvaluacionRepositorio evaluaciones;

	private ConsultarMiAvanceDeInformacionServicio servicio() {
		return new ConsultarMiAvanceDeInformacionServicio(importaciones, horarios, evaluaciones);
	}

	private static Importacion carga(String tipo, EstadoDeImportacion estado) {
		return new Importacion(1, tipo, "archivo.pdf", Instant.now(), estado, 3, 3);
	}

	private static ClaseDelHorario clase() {
		return new ClaseDelHorario(7, "CAD1", "Álgebra", new BloqueDeHorario(
				1, DiaDeLaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), null, null));
	}

	@Test
	void unRegistroExtendidoConfirmadoCuentaYUnoDeshechoNo() {
		given(horarios.clasesDe(ESTUDIANTE)).willReturn(List.of());
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(List.of());
		given(importaciones.deEstudiante(ESTUDIANTE)).willReturn(List.of(carga("historial", EstadoDeImportacion.REVERTIDA)));
		assertThat(servicio().ejecutar(ESTUDIANTE).pasos().get(0).hecho()).isFalse();

		given(importaciones.deEstudiante(ESTUDIANTE)).willReturn(List.of(carga("historial", EstadoDeImportacion.CONFIRMADA)));
		assertThat(servicio().ejecutar(ESTUDIANTE).pasos().get(0).hecho()).isTrue();
	}

	@Test
	void tenerClasesEnElHorarioCuentaComoElReporteDeHorarioCargado() {
		given(importaciones.deEstudiante(ESTUDIANTE)).willReturn(List.of());
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(List.of());
		given(horarios.clasesDe(ESTUDIANTE)).willReturn(List.of(clase()));

		assertThat(servicio().ejecutar(ESTUDIANTE).pasos().get(1).hecho()).isTrue();
	}

	@Test
	void unaActividadExtraNoCuentaComoElReporteDeHorarioCargado() {
		given(importaciones.deEstudiante(ESTUDIANTE)).willReturn(List.of());
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(List.of());
		var baile = ActividadExtra.nueva(
				ESTUDIANTE, "Baile", DiaDeLaSemana.LUNES, LocalTime.of(17, 0), LocalTime.of(19, 0), null, null);
		given(horarios.clasesDe(ESTUDIANTE)).willReturn(List.of(ClaseDelHorario.deActividadExtra(baile)));

		assertThat(servicio().ejecutar(ESTUDIANTE).pasos().get(1).hecho()).isFalse();
	}

	@Test
	void lasAsignaturasEnCursoSumanPasosSegunTenganActividades() {
		given(importaciones.deEstudiante(ESTUDIANTE)).willReturn(List.of(carga("historial", EstadoDeImportacion.CONFIRMADA)));
		given(horarios.clasesDe(ESTUDIANTE)).willReturn(List.of(clase()));
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(List.of(
				new AsignaturaMatriculada(7, "CAD1", "Álgebra", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 3, 12),
				new AsignaturaMatriculada(8, "CAD2", "Física", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 0, 0)));

		var avance = servicio().ejecutar(ESTUDIANTE);

		assertThat(avance.total()).isEqualTo(4);
		assertThat(avance.porcentaje()).isEqualTo(75);
	}
}
