package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EmisorDeTokensPort.TokenDeAcceso;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RenovarSesionServicioTest {

	private static final Instant INICIO = Instant.parse("2026-01-15T10:00:00Z");
	private static final Instant AHORA = INICIO.plus(Duration.ofHours(1));
	private static final OrigenDeSesion ORIGEN = new OrigenDeSesion("10.0.0.1", "Firefox");

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final EmisorDeTokensPort tokens = mock(EmisorDeTokensPort.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private RenovarSesionServicio servicio;

	private final Sesion vigente = new Sesion(1, 3, Sesion.huellaDe("refresco-viejo"), INICIO,
			INICIO.plus(Duration.ofDays(7)), null, null, "Firefox", "10.0.0.1");

	private Estudiante cuenta(EstadoCuenta estado) {
		return new Estudiante(1, "Ana", "Díaz", new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co"), estado, true, INICIO);
	}

	@BeforeEach
	void configurar() {
		servicio = new RenovarSesionServicio(estudiantes, sesiones, tokens, reloj, Duration.ofMinutes(20), Duration.ofDays(7));
		given(reloj.ahora()).willReturn(AHORA);
		given(estudiantes.buscarPorId(1)).willReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA)));
		given(tokens.generarTokenDeRefresco()).willReturn("refresco-nuevo");
		given(tokens.emitirAcceso(any(), any(), any())).willReturn(new TokenDeAcceso("jwt-nuevo", AHORA.plus(Duration.ofMinutes(20))));
		given(sesiones.buscarPorHuella(Sesion.huellaDe("refresco-viejo"))).willReturn(Optional.of(vigente));
	}

	@Test
	void cambiaElRefrescoPorUnoNuevoYRevocaElUsadoPorRotacion() {
		var renovada = servicio.ejecutar("refresco-viejo", ORIGEN);

		assertThat(renovada.tokenDeRefresco()).isEqualTo("refresco-nuevo");
		assertThat(renovada.tokenDeAcceso()).isEqualTo("jwt-nuevo");
		assertThat(renovada.refrescoExpira()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
		var revocada = ArgumentCaptor.forClass(Sesion.class);
		var nueva = ArgumentCaptor.forClass(Sesion.class);
		verify(sesiones).rotar(revocada.capture(), nueva.capture());
		assertThat(revocada.getValue().motivoRevocacion()).isEqualTo(MotivoDeRevocacion.ROTACION);
		assertThat(nueva.getValue().huellaRefresco()).isEqualTo(Sesion.huellaDe("refresco-nuevo"));
	}

	@Test
	void unRefrescoDesconocidoNoSirve() {
		assertThatThrownBy(() -> servicio.ejecutar("inventado", ORIGEN)).isInstanceOf(SesionInvalidaException.class);
		verify(sesiones, never()).rotar(any(), any());
	}

	@Test
	void usarDeNuevoUnRefrescoYaRotadoRevocaTodasLasSesionesDeLaCuenta() {
		given(sesiones.buscarPorHuella(Sesion.huellaDe("refresco-viejo")))
				.willReturn(Optional.of(vigente.revocar(MotivoDeRevocacion.ROTACION, INICIO.plusSeconds(60))));

		assertThatThrownBy(() -> servicio.ejecutar("refresco-viejo", ORIGEN)).isInstanceOf(SesionInvalidaException.class);

		verify(sesiones).revocarVigentes(1, MotivoDeRevocacion.REUSO_DETECTADO, AHORA);
		verify(sesiones, never()).rotar(any(), any());
	}

	@Test
	void unRefrescoDeUnaSesionCerradaSoloSeRechaza() {
		given(sesiones.buscarPorHuella(Sesion.huellaDe("refresco-viejo")))
				.willReturn(Optional.of(vigente.revocar(MotivoDeRevocacion.CIERRE_SESION, INICIO.plusSeconds(60))));

		assertThatThrownBy(() -> servicio.ejecutar("refresco-viejo", ORIGEN)).isInstanceOf(SesionInvalidaException.class);

		verify(sesiones, never()).revocarVigentes(anyInt(), any(), any());
	}

	@Test
	void unRefrescoVencidoNoSirve() {
		given(reloj.ahora()).willReturn(INICIO.plus(Duration.ofDays(8)));

		assertThatThrownBy(() -> servicio.ejecutar("refresco-viejo", ORIGEN)).isInstanceOf(SesionInvalidaException.class);
		verify(sesiones, never()).rotar(any(), any());
	}

	@Test
	void unaCuentaQueYaNoEstaActivaNoRenueva() {
		given(estudiantes.buscarPorId(1)).willReturn(Optional.of(cuenta(EstadoCuenta.INACTIVA)));

		assertThatThrownBy(() -> servicio.ejecutar("refresco-viejo", ORIGEN)).isInstanceOf(SesionInvalidaException.class);
	}

	private static int anyInt() {
		return org.mockito.ArgumentMatchers.anyInt();
	}
}
