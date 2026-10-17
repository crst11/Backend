package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula.ItemDeBrujula;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula.Resultado;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion.Configuracion;
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
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDelRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.OrigenDeCategoria;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotaRequerida;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotas;
import co.edu.ucundinamarca.cundiapp.domain.service.EvaluadorDeRiesgo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** La Brújula: qué asignaturas entran, en qué orden y que cambiar los umbrales no se guarda mal (SCRUM-35 y SCRUM-36). */
@ExtendWith(MockitoExtension.class)
class BrujulaServiciosTest {

	private static final int ESTUDIANTE = 5;

	@Mock
	private EstructuraDeEvaluacionRepositorio evaluaciones;

	@Mock
	private ConfiguracionRepositorio configuracion;

	private ConsultarMiBrujulaServicio brujula() {
		return new ConsultarMiBrujulaServicio(evaluaciones, configuracion,
				new CalculadoraDeNotaRequerida(new CalculadoraDeNotas()), new EvaluadorDeRiesgo());
	}

	private static AsignaturaMatriculada matricula(int id, String nombre, EstadoDeAsignatura estado) {
		return new AsignaturaMatriculada(id, "COD" + id, nombre, 3, "2026-2", estado, 1, 1);
	}

	/** Una sola actividad que vale la materia entera: la nota registrada es lo que lleva. */
	private static EstructuraDeEvaluacion estructura(int idMatricula) {
		return new EstructuraDeEvaluacion(idMatricula, List.of(new CategoriaDeEvaluacion(1, "Examen final",
				new BigDecimal("100"), OrigenDeCategoria.ESTUDIANTE, List.of(new ActividadEvaluativa(1, "Examen",
						new BigDecimal("100"), null, TipoDeActividad.PARCIAL, EstadoDeEntrega.NO_ENTREGADA)))));
	}

	private void conEstructura(int idMatricula, Map<ClaveDeActividad, Nota> notas) {
		given(evaluaciones.buscar(ESTUDIANTE, idMatricula)).willReturn(Optional.of(estructura(idMatricula)));
		given(evaluaciones.notasDe(ESTUDIANTE, idMatricula)).willReturn(new NotasDeLaAsignatura(idMatricula, notas));
	}

	@Test
	void ordenaLoMasUrgentePrimeroDejaAlFinalLoQueNoTieneDatosYOmiteLoQueNoSeCursa() {
		given(configuracion.metaDe(ESTUDIANTE)).willReturn(Meta.PREDETERMINADA);
		given(configuracion.umbralesDe(ESTUDIANTE)).willReturn(UmbralesDeRiesgo.PREDETERMINADOS);
		given(evaluaciones.matriculasDe(ESTUDIANTE)).willReturn(List.of(
				matricula(1, "Cálculo", EstadoDeAsignatura.EN_CURSO),
				matricula(2, "Redes", EstadoDeAsignatura.EN_CURSO),
				matricula(3, "Física", EstadoDeAsignatura.EN_CURSO),
				matricula(4, "Álgebra", EstadoDeAsignatura.APROBADA)));
		// Cálculo: 1.0 en el único examen, no queda nada por evaluar y la meta es 3.0: inalcanzable.
		conEstructura(1, Map.of(new ClaveDeActividad(1, 1), new Nota(new BigDecimal("1.0"))));
		// Redes: sin notas, hace falta 3.0 en todo: bajo.
		conEstructura(2, Map.of());
		// Física: todavía no dijo cómo la evalúan.
		given(evaluaciones.buscar(ESTUDIANTE, 3)).willReturn(Optional.empty());

		Resultado resultado = brujula().ejecutar(ESTUDIANTE);

		assertThat(resultado.asignaturas()).extracting(i -> i.asignatura().nombre())
				.containsExactly("Cálculo", "Redes", "Física");
		assertThat(resultado.asignaturas()).extracting(i -> i.riesgo().nivel())
				.containsExactly(NivelDeRiesgo.ALTO, NivelDeRiesgo.BAJO, NivelDeRiesgo.SIN_DATOS);
		assertThat(resultado.asignaturas().get(0).riesgo().motivo()).isEqualTo(MotivoDelRiesgo.INALCANZABLE);
		assertThat(resultado.meta()).isEqualTo(Meta.PREDETERMINADA);
		assertThat(resultado.umbrales()).isEqualTo(UmbralesDeRiesgo.PREDETERMINADOS);
	}

	@Test
	void conOtrosUmbralesLaMismaAsignaturaCambiaDeNivel() {
		given(configuracion.metaDe(ESTUDIANTE)).willReturn(Meta.PREDETERMINADA);
		given(configuracion.umbralesDe(ESTUDIANTE))
				.willReturn(new UmbralesDeRiesgo(new BigDecimal("2.00"), new BigDecimal("2.90")));
		given(evaluaciones.matriculasDe(ESTUDIANTE))
				.willReturn(List.of(matricula(2, "Redes", EstadoDeAsignatura.EN_CURSO)));
		conEstructura(2, Map.of());

		List<ItemDeBrujula> items = brujula().ejecutar(ESTUDIANTE).asignaturas();

		// Hace falta 3.0 y el umbral alto ahora es 2.9.
		assertThat(items.getFirst().riesgo().nivel()).isEqualTo(NivelDeRiesgo.ALTO);
		assertThat(items.getFirst().riesgo().motivo()).isEqualTo(MotivoDelRiesgo.SOBRE_EL_UMBRAL_ALTO);
	}

	@Test
	void sinAsignaturasEnCursoLaBrujulaVieneVacia() {
		given(configuracion.metaDe(ESTUDIANTE)).willReturn(Meta.PREDETERMINADA);
		given(configuracion.umbralesDe(ESTUDIANTE)).willReturn(UmbralesDeRiesgo.PREDETERMINADOS);
		given(evaluaciones.matriculasDe(ESTUDIANTE))
				.willReturn(List.of(matricula(4, "Álgebra", EstadoDeAsignatura.APROBADA)));

		assertThat(brujula().ejecutar(ESTUDIANTE).asignaturas()).isEmpty();
	}

	@Test
	void cambiarLosUmbralesLosGuardaYDevuelveLaConfiguracion() {
		UmbralesDeRiesgo nuevos = new UmbralesDeRiesgo(new BigDecimal("3.00"), new BigDecimal("4.00"));
		given(configuracion.metaDe(ESTUDIANTE)).willReturn(Meta.PREDETERMINADA);

		Configuracion resultado = new CambiarMisUmbralesServicio(configuracion).ejecutar(ESTUDIANTE, nuevos);

		verify(configuracion).guardarUmbrales(ESTUDIANTE, nuevos);
		assertThat(resultado.umbrales()).isEqualTo(nuevos);
		assertThat(resultado.meta()).isEqualTo(Meta.PREDETERMINADA);
	}

	@Test
	void cambiarSinUmbralesSeRechazaSinEscribir() {
		assertThatThrownBy(() -> new CambiarMisUmbralesServicio(configuracion).ejecutar(ESTUDIANTE, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Faltan los umbrales");

		verify(configuracion, never()).guardarUmbrales(any(Integer.class), any());
	}
}
