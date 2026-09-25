package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.GeneradorDeCodigoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmisorDeCodigoDeVerificacionTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final GeneradorDeCodigoPort generador = mock(GeneradorDeCodigoPort.class);
	private final EnviadorDeCodigoPort enviador = mock(EnviadorDeCodigoPort.class);
	private final CodigoDeVerificacionRepositorio repositorio = mock(CodigoDeVerificacionRepositorio.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private final EmisorDeCodigoDeVerificacion emisor =
			new EmisorDeCodigoDeVerificacion(generador, enviador, repositorio, reloj);

	private final Estudiante estudiante = new Estudiante(
			7, "Ana", "Díaz", new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co"),
			EstadoCuenta.PENDIENTE, true, AHORA);

	@Test
	void enviaElCodigoYGuardaSoloSuHuella() {
		given(generador.generar()).willReturn("482913");
		given(reloj.ahora()).willReturn(AHORA);

		emisor.emitirYEnviar(estudiante);

		verify(enviador).enviar(estudiante.correo(), "482913");
		var guardado = ArgumentCaptor.forClass(CodigoDeVerificacion.class);
		verify(repositorio).guardar(guardado.capture());
		assertThat(guardado.getValue().idEstudiante()).isEqualTo(7);
		assertThat(guardado.getValue().huella()).doesNotContain("482913");
	}

	@Test
	void siElEnvioFallaNoQuedaUnCodigoGuardado() {
		given(generador.generar()).willReturn("482913");
		doThrow(new IllegalStateException("sin correo")).when(enviador).enviar(any(), eq("482913"));

		assertThatThrownBy(() -> emisor.emitirYEnviar(estudiante)).isInstanceOf(IllegalStateException.class);
		verify(repositorio, never()).guardar(any());
	}
}
