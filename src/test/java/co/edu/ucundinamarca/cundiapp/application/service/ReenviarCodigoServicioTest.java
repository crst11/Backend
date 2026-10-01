package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReenviarCodigoServicioTest {

	private static final Instant EMISION = Instant.parse("2026-01-15T10:00:00Z");
	private static final String CORREO = "ana.diaz@ucundinamarca.edu.co";

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final CodigoDeVerificacionRepositorio codigos = mock(CodigoDeVerificacionRepositorio.class);
	private final EmisorDeCodigoDeVerificacion emisor = mock(EmisorDeCodigoDeVerificacion.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private ReenviarCodigoServicio servicio;

	private final Estudiante pendiente = new Estudiante(
			1, "Ana", "Díaz", new CorreoInstitucional(CORREO), EstadoCuenta.PENDIENTE, true, EMISION);

	@BeforeEach
	void configurar() {
		servicio = new ReenviarCodigoServicio(estudiantes, codigos, emisor, reloj);
		given(estudiantes.buscarPorCorreo(new CorreoInstitucional(CORREO))).willReturn(Optional.of(pendiente));
	}

	@Test
	void emiteUnCodigoNuevoSiElAnteriorVencio() {
		given(codigos.buscarDe(1, PropositoDelCodigo.VERIFICAR_CORREO)).willReturn(Optional.of(CodigoDeVerificacion.emitir(1, PropositoDelCodigo.VERIFICAR_CORREO, "123456", EMISION)));
		given(reloj.ahora()).willReturn(EMISION.plusSeconds(16 * 60));

		servicio.ejecutar(CORREO);

		verify(emisor).emitirYEnviar(pendiente, PropositoDelCodigo.VERIFICAR_CORREO);
	}

	@Test
	void emiteUnCodigoSiNuncaSeHabiaEmitido() {
		given(codigos.buscarDe(1, PropositoDelCodigo.VERIFICAR_CORREO)).willReturn(Optional.empty());
		given(reloj.ahora()).willReturn(EMISION);

		servicio.ejecutar(CORREO);

		verify(emisor).emitirYEnviar(pendiente, PropositoDelCodigo.VERIFICAR_CORREO);
	}

	@Test
	void rechazaPedirOtroEnSeguidaDeHaberPedidoUno() {
		given(codigos.buscarDe(1, PropositoDelCodigo.VERIFICAR_CORREO)).willReturn(Optional.of(
				CodigoDeVerificacion.emitir(1, PropositoDelCodigo.VERIFICAR_CORREO, "123456", EMISION)));
		given(reloj.ahora()).willReturn(EMISION.plusSeconds(30));

		assertThatThrownBy(() -> servicio.ejecutar(CORREO))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("sigue vigente");
		verify(emisor, never()).emitirYEnviar(any(), eq(PropositoDelCodigo.VERIFICAR_CORREO));
	}

	@Test
	void permitePedirOtroPasadaLaEsperaAunqueElAnteriorSigaVigente() {
		// El correo sale en segundo plano (SCRUM-67): si no llegó, esperar los 15 minutos de vigencia
		// dejaría a la persona atascada.
		given(codigos.buscarDe(1, PropositoDelCodigo.VERIFICAR_CORREO)).willReturn(Optional.of(
				CodigoDeVerificacion.emitir(1, PropositoDelCodigo.VERIFICAR_CORREO, "123456", EMISION)));
		given(reloj.ahora()).willReturn(EMISION.plus(CodigoDeVerificacion.ESPERA_PARA_REEMITIR));

		servicio.ejecutar(CORREO);

		verify(emisor).emitirYEnviar(any(), eq(PropositoDelCodigo.VERIFICAR_CORREO));
	}

	@Test
	void noHaceNadaNiRevelaSiElCorreoNoTieneCuentaPendiente() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.empty());

		servicio.ejecutar(CORREO);

		verify(emisor, never()).emitirYEnviar(any(), eq(PropositoDelCodigo.VERIFICAR_CORREO));
	}
}
