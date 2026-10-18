package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion.Configuracion;
import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota;
import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota.Resultado;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** La meta del estudiante y la proyección con sus notas reales (SCRUM-33 y SCRUM-34). */
@ExtendWith(MockitoExtension.class)
class ConfiguracionYProyeccionServiciosTest {

	private static final int ESTUDIANTE = 5;
	private static final int MATRICULA = 7;

	@Mock
	private ConfiguracionRepositorio configuracion;

	@Mock
	private SimularMiNota simulador;

	@Test
	void consultarDevuelveLaMetaGuardada() {
		given(configuracion.metaDe(ESTUDIANTE)).willReturn(new Meta(new BigDecimal("3.8")));
		given(configuracion.umbralesDe(ESTUDIANTE)).willReturn(UmbralesDeRiesgo.PREDETERMINADOS);

		Configuracion resultado = new ConsultarMiConfiguracionServicio(configuracion).ejecutar(ESTUDIANTE);

		assertThat(resultado.meta()).isEqualTo(new Meta(new BigDecimal("3.8")));
		assertThat(resultado.umbrales()).isEqualTo(UmbralesDeRiesgo.PREDETERMINADOS);
	}

	@Test
	void cambiarGuardaLaMetaYDevuelveLaNueva() {
		Meta nueva = new Meta(new BigDecimal("4.2"));
		given(configuracion.umbralesDe(ESTUDIANTE)).willReturn(UmbralesDeRiesgo.PREDETERMINADOS);

		Configuracion resultado = new CambiarMiMetaServicio(configuracion).ejecutar(ESTUDIANTE, nueva);

		verify(configuracion).guardarMeta(ESTUDIANTE, nueva);
		assertThat(resultado.meta()).isEqualTo(nueva);
	}

	@Test
	void cambiarSinMetaSeRechazaSinEscribir() {
		assertThatThrownBy(() -> new CambiarMiMetaServicio(configuracion).ejecutar(ESTUDIANTE, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Falta la meta");

		verify(configuracion, never()).guardarMeta(any(Integer.class), any());
	}

	@Test
	void laProyeccionConNotasRealesEsUnaSimulacionSinNotasHipoteticas() {
		Meta meta = new Meta(new BigDecimal("3.5"));
		Resultado esperado = new Resultado(null, null);
		given(simulador.ejecutar(ESTUDIANTE, MATRICULA, meta, List.of())).willReturn(esperado);

		Resultado resultado = new ConsultarMiProyeccionDeNotaServicio(simulador).ejecutar(ESTUDIANTE, MATRICULA, meta);

		assertThat(resultado).isSameAs(esperado);
	}
}
