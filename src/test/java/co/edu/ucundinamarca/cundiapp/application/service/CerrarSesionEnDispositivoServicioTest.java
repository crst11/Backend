package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerrarSesionEnDispositivoServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private CerrarSesionEnDispositivoServicio servicio;

	@BeforeEach
	void configurar() {
		servicio = new CerrarSesionEnDispositivoServicio(sesiones, reloj);
		given(reloj.ahora()).willReturn(AHORA);
	}

	@Test
	void cierraLaSesionIndicada() {
		given(sesiones.revocarUna(7, 2, MotivoDeRevocacion.CIERRE_SESION, AHORA)).willReturn(true);

		assertThatCode(() -> servicio.ejecutar(7, 2)).doesNotThrowAnyException();

		verify(sesiones).revocarUna(7, 2, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}

	@Test
	void avisaSiEsaSesionYaNoEstabaAbierta() {
		given(sesiones.revocarUna(anyInt(), anyInt(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any())).willReturn(false);

		assertThatThrownBy(() -> servicio.ejecutar(7, 99))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("ya no está abierta");
	}

	@Test
	void unConsecutivoDeOtraPersonaNoCierraNada() {
		// El id sale del token: pedir el consecutivo de alguien más no encuentra una sesión propia abierta.
		given(sesiones.revocarUna(anyInt(), anyInt(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any())).willReturn(false);

		assertThatThrownBy(() -> servicio.ejecutar(7, 1)).isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(sesiones).revocarUna(7, 1, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}

	@Test
	void cerrarTodasRevocaTambienLaDeQuienLoPide() {
		servicio.ejecutarTodas(7);

		verify(sesiones).revocarVigentes(7, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}
}
