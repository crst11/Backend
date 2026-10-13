package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Qué asignaturas se ofrecen en *Mis notas*: solo las del semestre actual (SCRUM-86). */
@ExtendWith(MockitoExtension.class)
class ListarMisAsignaturasMatriculadasServicioTest {

	private static final int ESTUDIANTE = 5;

	@Mock
	private EstructuraDeEvaluacionRepositorio evaluaciones;

	private static AsignaturaMatriculada asignatura(int id, String periodo, EstadoDeAsignatura estado) {
		return new AsignaturaMatriculada(id, "CAD" + id, "Asignatura " + id, 3, periodo, estado, 0, 0);
	}

	@Test
	void sinCriterioDevuelveTodasLasMatriculadas() {
		var todas = List.of(
				asignatura(1, "2024-2", EstadoDeAsignatura.APROBADA), asignatura(2, "2026-2", EstadoDeAsignatura.EN_CURSO));
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(todas);

		assertThat(new ListarMisAsignaturasMatriculadasServicio(evaluaciones).ejecutar(ESTUDIANTE)).isEqualTo(todas);
	}

	@Test
	void delSemestreActualSoloDevuelveLasQueEstaCursando() {
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(List.of(
				asignatura(1, "2024-2", EstadoDeAsignatura.APROBADA),
				asignatura(2, "2026-2", EstadoDeAsignatura.EN_CURSO),
				asignatura(3, "2025-1", EstadoDeAsignatura.REPROBADA),
				asignatura(4, "2026-2", EstadoDeAsignatura.EN_CURSO)));

		var actuales = new ListarMisAsignaturasMatriculadasServicio(evaluaciones).delSemestreActual(ESTUDIANTE);

		assertThat(actuales).extracting(AsignaturaMatriculada::idMatricula).containsExactly(2, 4);
	}

	@Test
	void sinNingunaEnCursoDevuelveUnaListaVaciaNoUnError() {
		given(evaluaciones.matriculasDe(ESTUDIANTE))
				.willReturn(List.of(asignatura(1, "2024-2", EstadoDeAsignatura.APROBADA)));

		assertThat(new ListarMisAsignaturasMatriculadasServicio(evaluaciones).delSemestreActual(ESTUDIANTE)).isEmpty();
	}
}
