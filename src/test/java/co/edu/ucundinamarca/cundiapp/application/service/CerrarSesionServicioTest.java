package co.edu.ucundinamarca.cundiapp.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerrarSesionServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private CerrarSesionServicio servicio;

	private final Sesion abierta = new Sesion(1, 2, Sesion.huellaDe("refresco"), AHORA.minusSeconds(60),
			AHORA.plus(Duration.ofDays(7)), null, null, "Firefox", "10.0.0.1");

	@BeforeEach
	void configurar() {
		servicio = new CerrarSesionServicio(sesiones, reloj);
		given(reloj.ahora()).willReturn(AHORA);
	}

	@Test
	void revocaLaSesionConMotivoCierreDeSesion() {
		given(sesiones.buscarPorHuella(Sesion.huellaDe("refresco"))).willReturn(Optional.of(abierta));

		servicio.ejecutar("refresco");

		verify(sesiones).actualizar(abierta.revocar(MotivoDeRevocacion.CIERRE_SESION, AHORA));
	}

	@Test
	void cerrarDosVecesNoFallaYNoVuelveARevocar() {
		given(sesiones.buscarPorHuella(Sesion.huellaDe("refresco")))
				.willReturn(Optional.of(abierta.revocar(MotivoDeRevocacion.CIERRE_SESION, AHORA.minusSeconds(5))));

		servicio.ejecutar("refresco");

		verify(sesiones, never()).actualizar(any());
	}

	@Test
	void unTokenDesconocidoNoHaceNada() {
		given(sesiones.buscarPorHuella(any())).willReturn(Optional.empty());

		servicio.ejecutar("inventado");

		verify(sesiones, never()).actualizar(any());
	}
}
