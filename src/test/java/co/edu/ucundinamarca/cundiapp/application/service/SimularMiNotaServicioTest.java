package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota.NotaSimulada;
import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota.Resultado;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEntrega;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.OrigenDeCategoria;
import co.edu.ucundinamarca.cundiapp.domain.model.SituacionDeLaMeta;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotaRequerida;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotas;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** La nota que hace falta y la simulación: qué se comprueba, qué se lee y que nada se escribe (SCRUM-33 y SCRUM-34). */
@ExtendWith(MockitoExtension.class)
class SimularMiNotaServicioTest {

	private static final int ESTUDIANTE = 5;
	private static final int MATRICULA = 7;
	private static final ClaveDeActividad PARCIAL = new ClaveDeActividad(1, 1);

	@Mock
	private EstructuraDeEvaluacionRepositorio evaluaciones;

	@Mock
	private ConfiguracionRepositorio configuracion;

	private SimularMiNotaServicio servicio() {
		return new SimularMiNotaServicio(evaluaciones, configuracion,
				new CalculadoraDeNotaRequerida(new CalculadoraDeNotas()));
	}

	private static EstructuraDeEvaluacion estructura() {
		return new EstructuraDeEvaluacion(MATRICULA, List.of(new CategoriaDeEvaluacion(1, "Primer corte",
				new BigDecimal("100"), OrigenDeCategoria.ESTUDIANTE, List.of(new ActividadEvaluativa(1, "Parcial",
						new BigDecimal("100"), null, TipoDeActividad.PARCIAL, EstadoDeEntrega.NO_ENTREGADA)))));
	}

	private static AsignaturaMatriculada matricula() {
		return new AsignaturaMatriculada(MATRICULA, "CAD1", "Álgebra", 3, "2024-2", EstadoDeAsignatura.APROBADA, 1, 1);
	}

	private static NotaSimulada simulada(ClaveDeActividad clave, String nota) {
		return new NotaSimulada(clave, new Nota(new BigDecimal(nota)));
	}

	private void conEstructura() {
		given(evaluaciones.matricula(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(matricula()));
		given(evaluaciones.buscar(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(estructura()));
	}

	private void conEstructuraYSinNotas() {
		conEstructura();
		given(evaluaciones.notasDe(ESTUDIANTE, MATRICULA)).willReturn(new NotasDeLaAsignatura(MATRICULA, Map.of()));
	}

	@Test
	void unaMatriculaAjenaSeTrataComoSiNoExistiera() {
		given(evaluaciones.matricula(ESTUDIANTE, MATRICULA)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, null, List.of()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("No encontramos esa asignatura");

		verify(evaluaciones, never()).notasDe(any(Integer.class), any(Integer.class));
		verifyNoInteractions(configuracion);
	}

	@Test
	void sinEstructuraGuardadaNoSeProyectaYSeDiceQueHaceFalta() {
		given(evaluaciones.matricula(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(matricula()));
		given(evaluaciones.buscar(ESTUDIANTE, MATRICULA)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, null, List.of()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Define primero cómo te evalúan");
	}

	@Test
	void unaEstructuraSinCategoriasTampocoSeProyecta() {
		given(evaluaciones.matricula(ESTUDIANTE, MATRICULA)).willReturn(Optional.of(matricula()));
		given(evaluaciones.buscar(ESTUDIANTE, MATRICULA))
				.willReturn(Optional.of(new EstructuraDeEvaluacion(MATRICULA, List.of())));

		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, null, List.of()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Define primero cómo te evalúan");
	}

	@Test
	void sinMetaEnLaPeticionUsaLaGuardadaDelEstudiante() {
		conEstructuraYSinNotas();
		given(configuracion.metaDe(ESTUDIANTE)).willReturn(new Meta(new BigDecimal("3.5")));

		Resultado resultado = servicio().ejecutar(ESTUDIANTE, MATRICULA, null, List.of());

		assertThat(resultado.proyeccion().meta()).isEqualTo(new Meta(new BigDecimal("3.5")));
		assertThat(resultado.asignatura().nombre()).isEqualTo("Álgebra");
		assertThat(resultado.proyeccion().requerida())
				.hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("3.50"));
	}

	@Test
	void conMetaEnLaPeticionLaUsaSinConsultarNiGuardarLaDelEstudiante() {
		conEstructuraYSinNotas();

		Resultado resultado = servicio().ejecutar(ESTUDIANTE, MATRICULA, new Meta(new BigDecimal("4.0")), List.of());

		assertThat(resultado.proyeccion().meta()).isEqualTo(new Meta(new BigDecimal("4.0")));
		verifyNoInteractions(configuracion);
	}

	@Test
	void unaNotaHipoteticaCambiaLaProyeccionSinEscribirNada() {
		conEstructuraYSinNotas();

		Resultado resultado = servicio().ejecutar(ESTUDIANTE, MATRICULA, new Meta(new BigDecimal("3.0")),
				List.of(simulada(PARCIAL, "4.0")));

		assertThat(resultado.proyeccion().situacion()).isEqualTo(SituacionDeLaMeta.ALCANZADA);
		assertThat(resultado.proyeccion().puntos()).isEqualByComparingTo("4.00");
		verify(evaluaciones, never()).guardarNota(any(Integer.class), any(Integer.class), any(), any());
		verify(evaluaciones, never()).guardar(any(Integer.class), any());
	}

	@Test
	void unaNotaParaUnaActividadQueNoExisteSeRechaza() {
		conEstructura();

		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, new Meta(new BigDecimal("3.0")),
				List.of(simulada(new ClaveDeActividad(1, 9), "4.0"))))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("no está en la estructura");
	}

	@Test
	void dosNotasParaLaMismaActividadSeRechazan() {
		conEstructura();

		assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, MATRICULA, new Meta(new BigDecimal("3.0")),
				List.of(simulada(PARCIAL, "4.0"), simulada(PARCIAL, "2.0"))))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("la misma actividad");
	}
}
