package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.ucundinamarca.cundiapp.application.port.in.SugerenciaParaMatricular;
import co.edu.ucundinamarca.cundiapp.application.port.in.SugerenciaParaMatricular.Razon;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HistorialAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaCursada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.HistorialAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoCursado;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.service.SugeridorDeMatricula;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El caso de uso que arma la sugerencia de matrícula con lo que el estudiante tiene guardado (SCRUM-244). */
class SugerirMatriculaServicioTest {

	private static final int ESTUDIANTE = 7;
	private static final String CODIGO_PLAN = "PLAN";

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final ProgramaAcademicoRepositorio programas = mock(ProgramaAcademicoRepositorio.class);
	private final HistorialAcademicoRepositorio historiales = mock(HistorialAcademicoRepositorio.class);
	private final SugerirMatriculaServicio servicio =
			new SugerirMatriculaServicio(estudiantes, programas, historiales, new SugeridorDeMatricula());

	private static PlanDeEstudios plan() {
		ProgramaAcademico programa = new ProgramaAcademico("P1", "Ingeniería", "Facultad", "Sede", 100, 10);
		return new PlanDeEstudios(CODIGO_PLAN, programa, List.of(
				new Asignatura("A", "Materia A", 3, TipoDeAsignatura.OBLIGATORIA, 1, List.of()),
				new Asignatura("B", "Materia B", 3, TipoDeAsignatura.OBLIGATORIA, 2, List.of("A"))));
	}

	private static HistorialAcademico historial(EstadoDeAsignatura estadoDeA, EstadoDeAsignatura estadoDeB) {
		return new HistorialAcademico(List.of(new PeriodoCursado("2026-1", List.of(
				cursada("A", estadoDeA), cursada("B", estadoDeB)))));
	}

	private static AsignaturaCursada cursada(String codigo, EstadoDeAsignatura estado) {
		BigDecimal nota = estado == EstadoDeAsignatura.EN_CURSO ? null : new BigDecimal("4.0");
		return new AsignaturaCursada(codigo, "Materia " + codigo, 3, nota, estado);
	}

	@Test
	void sinPlanElegidoExplicaQueFaltaElPrograma() {
		when(estudiantes.planDe(ESTUDIANTE)).thenReturn(Optional.empty());

		SugerenciaParaMatricular resultado = servicio.ejecutar(ESTUDIANTE);

		assertThat(resultado.sugerencia()).isNull();
		assertThat(resultado.razon()).isEqualTo(Razon.SIN_PLAN);
	}

	@Test
	void conUnPlanQueYaNoExisteTambienExplicaQueFaltaElPrograma() {
		when(estudiantes.planDe(ESTUDIANTE)).thenReturn(Optional.of(CODIGO_PLAN));
		when(programas.buscarPlanPorCodigo(CODIGO_PLAN)).thenReturn(Optional.empty());

		assertThat(servicio.ejecutar(ESTUDIANTE).razon()).isEqualTo(Razon.SIN_PLAN);
	}

	@Test
	void sinHistorialImportadoExplicaQueFaltaImportar() {
		when(estudiantes.planDe(ESTUDIANTE)).thenReturn(Optional.of(CODIGO_PLAN));
		when(programas.buscarPlanPorCodigo(CODIGO_PLAN)).thenReturn(Optional.of(plan()));
		when(historiales.historialDe(ESTUDIANTE)).thenReturn(new HistorialAcademico(List.of()));

		SugerenciaParaMatricular resultado = servicio.ejecutar(ESTUDIANTE);

		assertThat(resultado.sugerencia()).isNull();
		assertThat(resultado.razon()).isEqualTo(Razon.SIN_HISTORIAL);
	}

	@Test
	void conPlanEHistorialDevuelveLaSugerenciaSinRazon() {
		when(estudiantes.planDe(ESTUDIANTE)).thenReturn(Optional.of(CODIGO_PLAN));
		when(programas.buscarPlanPorCodigo(CODIGO_PLAN)).thenReturn(Optional.of(plan()));
		when(historiales.historialDe(ESTUDIANTE))
				.thenReturn(historial(EstadoDeAsignatura.APROBADA, EstadoDeAsignatura.CANCELADA));

		SugerenciaParaMatricular resultado = servicio.ejecutar(ESTUDIANTE);

		assertThat(resultado.razon()).isNull();
		assertThat(resultado.sugerencia().semestreSugerido()).isEqualTo(2);
		assertThat(resultado.sugerencia().materias()).extracting("codigo").containsExactly("B");
	}

	@Test
	void siYaNoQuedaNadaPorMatricularLoDice() {
		when(estudiantes.planDe(ESTUDIANTE)).thenReturn(Optional.of(CODIGO_PLAN));
		when(programas.buscarPlanPorCodigo(CODIGO_PLAN)).thenReturn(Optional.of(plan()));
		when(historiales.historialDe(ESTUDIANTE))
				.thenReturn(historial(EstadoDeAsignatura.APROBADA, EstadoDeAsignatura.EN_CURSO));

		SugerenciaParaMatricular resultado = servicio.ejecutar(ESTUDIANTE);

		assertThat(resultado.razon()).isEqualTo(Razon.NADA_POR_MATRICULAR);
		assertThat(resultado.sugerencia().materias()).isEmpty();
	}
}
