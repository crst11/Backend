package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.MetodoDeAcceso;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CerrarSesionEnDispositivoServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");
	private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0) Chrome/141";
	private static final String ANDROID = "Mozilla/5.0 (Linux; Android 14) Chrome/141";

	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private CerrarSesionEnDispositivoServicio servicio;

	private static Sesion sesion(int consecutivo, String userAgent, Duration antiguedad) {
		Instant inicio = AHORA.minus(antiguedad);
		return new Sesion(7, consecutivo, MetodoDeAcceso.LOCAL, "huella" + consecutivo, inicio,
				inicio.plus(Duration.ofDays(7)), null, null, userAgent, "10.0.0.1");
	}

	@BeforeEach
	void configurar() {
		servicio = new CerrarSesionEnDispositivoServicio(sesiones, reloj);
		given(reloj.ahora()).willReturn(AHORA);
	}

	@Test
	void cierraLaSesionIndicada() {
		given(sesiones.listarVigentes(7, AHORA)).willReturn(List.of(sesion(2, CHROME, Duration.ZERO)));

		assertThatCode(() -> servicio.ejecutar(7, 2)).doesNotThrowAnyException();

		verify(sesiones).revocarUna(7, 2, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}

	@Test
	void cerrarUnDispositivoCierraTodaSuCadenaDeRotaciones() {
		// Revocar solo la última dejaría abiertas las anteriores y el dispositivo seguiría dentro.
		given(sesiones.listarVigentes(7, AHORA)).willReturn(List.of(
				sesion(5, CHROME, Duration.ZERO),
				sesion(4, CHROME, Duration.ofMinutes(20)),
				sesion(3, CHROME, Duration.ofMinutes(40))));

		servicio.ejecutar(7, 5);

		verify(sesiones).revocarUna(7, 3, MotivoDeRevocacion.CIERRE_SESION, AHORA);
		verify(sesiones).revocarUna(7, 4, MotivoDeRevocacion.CIERRE_SESION, AHORA);
		verify(sesiones).revocarUna(7, 5, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}

	@Test
	void cerrarUnDispositivoNoTocaLosDemas() {
		given(sesiones.listarVigentes(7, AHORA)).willReturn(List.of(
				sesion(5, CHROME, Duration.ZERO),
				sesion(2, ANDROID, Duration.ofHours(3))));

		servicio.ejecutar(7, 5);

		verify(sesiones).revocarUna(7, 5, MotivoDeRevocacion.CIERRE_SESION, AHORA);
		verify(sesiones, never()).revocarUna(7, 2, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}

	@Test
	void sePuedeCerrarUnDispositivoPorCualquieraDeSusSesiones() {
		// La pantalla manda el consecutivo más reciente, pero uno viejo señala al mismo dispositivo.
		given(sesiones.listarVigentes(7, AHORA)).willReturn(List.of(
				sesion(5, CHROME, Duration.ZERO),
				sesion(4, CHROME, Duration.ofMinutes(20))));

		servicio.ejecutar(7, 4);

		verify(sesiones).revocarUna(7, 4, MotivoDeRevocacion.CIERRE_SESION, AHORA);
		verify(sesiones).revocarUna(7, 5, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}

	@Test
	void avisaSiEsaSesionYaNoEstabaAbierta() {
		given(sesiones.listarVigentes(7, AHORA)).willReturn(List.of(sesion(2, CHROME, Duration.ZERO)));

		assertThatThrownBy(() -> servicio.ejecutar(7, 99))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("ya no está abierta");
	}

	@Test
	void unConsecutivoDeOtraPersonaNoCierraNada() {
		// El id sale del token: pedir el consecutivo de alguien más no encuentra una sesión propia abierta.
		given(sesiones.listarVigentes(7, AHORA)).willReturn(List.of(sesion(2, CHROME, Duration.ZERO)));

		assertThatThrownBy(() -> servicio.ejecutar(7, 1)).isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(sesiones, never()).revocarUna(anyInt(), anyInt(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void cerrarTodasRevocaTambienLaDeQuienLoPide() {
		servicio.ejecutarTodas(7);

		verify(sesiones).revocarVigentes(7, MotivoDeRevocacion.CIERRE_SESION, AHORA);
	}
}
