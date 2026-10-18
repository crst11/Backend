package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMiMeta;
import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMisUmbrales;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion.Configuracion;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RevisarCambiosDeRiesgo;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Revisar el riesgo después de cada acción que lo mueve, sin tumbar la acción (RF10, SCRUM-50). */
class RiesgoVigiladoTest {

	private static final int ESTUDIANTE = 5;
	private static final int MATRICULA = 11;

	private final RevisarCambiosDeRiesgo revisar = mock(RevisarCambiosDeRiesgo.class);
	private final RiesgoVigilado vigilado = new RiesgoVigilado(revisar);

	@Test
	void despuesDeAnotarUnaNotaRevisaEsaAsignatura() {
		RegistrarMiCalificacion caso = mock(RegistrarMiCalificacion.class);
		var esperado = mock(RegistrarMiCalificacion.Resultado.class);
		given(caso.ejecutar(ESTUDIANTE, MATRICULA, 1, 1, BigDecimal.ONE)).willReturn(esperado);

		var resultado = vigilado.vigilar(caso).ejecutar(ESTUDIANTE, MATRICULA, 1, 1, BigDecimal.ONE);

		assertThat(resultado).isSameAs(esperado);
		verify(revisar).deLaAsignatura(ESTUDIANTE, MATRICULA);
	}

	@Test
	void despuesDeQuitarUnaNotaRevisaEsaAsignatura() {
		QuitarMiCalificacion caso = mock(QuitarMiCalificacion.class);
		var esperado = mock(NotaAcumulada.class);
		given(caso.ejecutar(ESTUDIANTE, MATRICULA, 1, 1)).willReturn(esperado);

		var resultado = vigilado.vigilar(caso).ejecutar(ESTUDIANTE, MATRICULA, 1, 1);

		assertThat(resultado).isSameAs(esperado);
		verify(revisar).deLaAsignatura(ESTUDIANTE, MATRICULA);
	}

	@Test
	void despuesDeCambiarLaMetaRevisaTodasLasAsignaturas() {
		CambiarMiMeta caso = mock(CambiarMiMeta.class);
		var configuracion = new Configuracion(Meta.PREDETERMINADA, UmbralesDeRiesgo.PREDETERMINADOS);
		given(caso.ejecutar(ESTUDIANTE, Meta.PREDETERMINADA)).willReturn(configuracion);

		var resultado = vigilado.vigilar(caso).ejecutar(ESTUDIANTE, Meta.PREDETERMINADA);

		assertThat(resultado).isSameAs(configuracion);
		verify(revisar).deTodasLasAsignaturas(ESTUDIANTE);
	}

	@Test
	void despuesDeCambiarLosUmbralesRevisaTodasLasAsignaturas() {
		CambiarMisUmbrales caso = mock(CambiarMisUmbrales.class);
		var configuracion = new Configuracion(Meta.PREDETERMINADA, UmbralesDeRiesgo.PREDETERMINADOS);
		given(caso.ejecutar(ESTUDIANTE, UmbralesDeRiesgo.PREDETERMINADOS)).willReturn(configuracion);

		vigilado.vigilar(caso).ejecutar(ESTUDIANTE, UmbralesDeRiesgo.PREDETERMINADOS);

		verify(revisar).deTodasLasAsignaturas(ESTUDIANTE);
	}

	@Test
	void siRevisarFallaLaAccionSalePorIgual() {
		RegistrarMiCalificacion caso = mock(RegistrarMiCalificacion.class);
		var esperado = mock(RegistrarMiCalificacion.Resultado.class);
		given(caso.ejecutar(ESTUDIANTE, MATRICULA, 1, 1, BigDecimal.ONE)).willReturn(esperado);
		doThrow(new IllegalStateException("la base de datos no responde")).when(revisar).deLaAsignatura(ESTUDIANTE, MATRICULA);

		var resultado = vigilado.vigilar(caso).ejecutar(ESTUDIANTE, MATRICULA, 1, 1, BigDecimal.ONE);

		assertThat(resultado).isSameAs(esperado);
	}

	@Test
	void siLaAccionSeRechazaNoSeRevisaNadaYElErrorSale() {
		CambiarMiMeta caso = mock(CambiarMiMeta.class);
		given(caso.ejecutar(ESTUDIANTE, null)).willThrow(new ReglaDeNegocioVioladaException("Falta la meta"));

		assertThatThrownBy(() -> vigilado.vigilar(caso).ejecutar(ESTUDIANTE, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(revisar, never()).deTodasLasAsignaturas(ESTUDIANTE);
	}
}
