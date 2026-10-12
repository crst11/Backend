package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEntrega;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.OrigenDeCategoria;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotas;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Registrar una nota: el orden de las comprobaciones y lo que se escribe (SCRUM-28). */
@ExtendWith(MockitoExtension.class)
class RegistrarMiCalificacionServicioTest {

	private static final int ESTUDIANTE = 5;
	private static final int MATRICULA = 7;

	@Mock
	private EstructuraDeEvaluacionRepositorio evaluaciones;

	private RegistrarMiCalificacionServicio servicio() {
		return new RegistrarMiCalificacionServicio(evaluaciones, new CalculadoraDeNotas());
	}

	private static EstructuraDeEvaluacion estructura() {
		return new EstructuraDeEvaluacion(MATRICULA, List.of(new CategoriaDeEvaluacion(1, "Primer corte",
				new BigDecimal("100"), OrigenDeCategoria.ESTUDIANTE, List.of(new ActividadEvaluativa(1, "Parcial",
						new BigDecimal("100"), null, TipoDeActividad.PARCIAL, EstadoDeEntrega.NO_ENTREGADA)))));
	}

	private static AsignaturaMatriculada matricula() {
		return new AsignaturaMatriculada(MATRICULA, "CAD1", "Álgebra", 3, "2024-2", EstadoDeAsignatura.APROBADA, 1, 1);
	}

	@Test
	void unaNotaMalEscritaSeRechazaSinTocarLaBaseDeDatos() {
		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, 1, 1, new BigDecimal("7")))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		verifyNoInteractions(evaluaciones);
	}

	@Test
	void guardaLaNotaYDevuelveElAcumuladoConEsaNota() {
		given(evaluaciones.matricula(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(matricula()));
		given(evaluaciones.buscar(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(estructura()));
		given(evaluaciones.notasDe(ESTUDIANTE, MATRICULA)).willReturn(new NotasDeLaAsignatura(MATRICULA,
				Map.of(new ClaveDeActividad(1, 1), new Nota(new BigDecimal("4.0")))));

		var resultado = servicio().ejecutar(ESTUDIANTE, MATRICULA, 1, 1, new BigDecimal("4.0"));

		verify(evaluaciones).guardarNota(ESTUDIANTE, MATRICULA, new ClaveDeActividad(1, 1), new Nota(new BigDecimal("4.0")));
		assertThat(resultado.acumulado().puntos()).isEqualByComparingTo("4.00");
		assertThat(resultado.acumulado().actividadesCalificadas()).isEqualTo(1);
	}

	@Test
	void unaActividadQueNoExisteNoEscribeNada() {
		given(evaluaciones.matricula(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(matricula()));
		given(evaluaciones.buscar(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(estructura()));

		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, 1, 9, new BigDecimal("4.0")))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("no existe");

		verify(evaluaciones, never()).guardarNota(any(Integer.class), any(Integer.class), any(), any());
	}

	@Test
	void unaMatriculaQueNoEsDelEstudianteSeDiceComoNoEncontrada() {
		given(evaluaciones.matricula(ESTUDIANTE, MATRICULA)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, 1, 1, new BigDecimal("4.0")))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("No encontramos esa asignatura");
	}
}
