package co.edu.ucundinamarca.cundiapp.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Orquestación probada con dobles de los puertos: sin Spring, sin base de datos. */
class SolicitarRecuperacionServicioTest {

	private static final String CORREO = "ana.diaz@ucundinamarca.edu.co";
	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final CodigoDeVerificacionRepositorio codigos = mock(CodigoDeVerificacionRepositorio.class);
	private final EmisorDeCodigoDeVerificacion emisor = mock(EmisorDeCodigoDeVerificacion.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private SolicitarRecuperacionServicio servicio;

	private final Estudiante activa = new Estudiante(
			1, "Ana", "Díaz", new CorreoInstitucional(CORREO), EstadoCuenta.ACTIVA, true, AHORA);

	@BeforeEach
	void configurar() {
		servicio = new SolicitarRecuperacionServicio(estudiantes, codigos, emisor, reloj);
		given(reloj.ahora()).willReturn(AHORA);
	}

	@Test
	void enviaElCodigoDeRecuperacionAUnaCuentaActivaConContrasenaPropia() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(activa));
		given(estudiantes.contrasenaCifradaDe(1)).willReturn(Optional.of("hash-actual"));
		given(codigos.buscarDe(1, PropositoDelCodigo.RECUPERAR_CONTRASENA)).willReturn(Optional.empty());

		servicio.ejecutar(CORREO);

		verify(emisor).emitirYEnviar(activa, PropositoDelCodigo.RECUPERAR_CONTRASENA);
	}

	@Test
	void noHaceNadaNiRevelaSiElCorreoNoTieneCuenta() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.empty());

		servicio.ejecutar(CORREO);

		verify(emisor, never()).emitirYEnviar(any(), any());
	}

	@Test
	void unaCuentaPendienteNoRecuperaLaContrasena() {
		var pendiente = new Estudiante(
				1, "Ana", "Díaz", new CorreoInstitucional(CORREO), EstadoCuenta.PENDIENTE, true, AHORA);
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(pendiente));

		servicio.ejecutar(CORREO);

		verify(emisor, never()).emitirYEnviar(any(), any());
	}

	@Test
	void unaCuentaQueSoloEntraConGoogleNoTieneContrasenaQueRecuperar() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(activa));
		given(estudiantes.contrasenaCifradaDe(1)).willReturn(Optional.empty());

		servicio.ejecutar(CORREO);

		verify(emisor, never()).emitirYEnviar(any(), any());
	}

	@Test
	void noMandaOtroCorreoSiElCodigoAnteriorSigueSirviendo() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(activa));
		given(estudiantes.contrasenaCifradaDe(1)).willReturn(Optional.of("hash-actual"));
		given(codigos.buscarDe(1, PropositoDelCodigo.RECUPERAR_CONTRASENA)).willReturn(
				Optional.of(CodigoDeVerificacion.emitir(1, PropositoDelCodigo.RECUPERAR_CONTRASENA, "123456", AHORA)));

		servicio.ejecutar(CORREO);

		verify(emisor, never()).emitirYEnviar(any(), any());
	}
}
