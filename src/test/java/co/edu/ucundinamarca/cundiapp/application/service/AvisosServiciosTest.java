package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio.PendientePorAvisar;
import co.edu.ucundinamarca.cundiapp.application.port.out.DispositivoConAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.EnviadorDePushPort.CargaDePush;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEnvio;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeEnvioPush;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAviso;
import co.edu.ucundinamarca.cundiapp.domain.service.RedactorDeAvisos;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los casos de uso de los avisos (RF10, SCRUM-37): qué se genera, a quién se entrega y qué pasa cuando la
 * entrega falla. Todo con dobles de los puertos, sin base de datos ni red.
 */
@ExtendWith(MockitoExtension.class)
class AvisosServiciosTest {

	private static final int ESTUDIANTE = 5;
	private static final Instant AHORA = Instant.parse("2026-10-14T15:00:00Z");
	private static final RelojPort RELOJ = () -> AHORA;
	private static final ClavesDePushDePrueba.Suscripcion CLAVES = ClavesDePushDePrueba.nueva();

	@Mock
	private AvisoRepositorio avisos;

	@Mock
	private DispositivoConAvisosRepositorio dispositivos;

	@Mock
	private EnviadorDePushPort enviador;

	private static SuscripcionPush suscripcion(String sufijo) {
		return new SuscripcionPush("https://fcm.googleapis.com/fcm/send/" + sufijo, CLAVES.clavePublica(), CLAVES.secretoEnBase64());
	}

	private static DispositivoConAvisos dispositivo(int consecutivo) {
		return new DispositivoConAvisos(consecutivo, "Chrome en Android", suscripcion("d" + consecutivo), true, AHORA);
	}

	private static Aviso aviso(int id, TipoDeAviso tipo) {
		return new Aviso(id, ESTUDIANTE, tipo, "Vence pronto", "«Taller de redes» vence hoy a las 11:59 p. m.", AHORA,
				EstadoDeEnvio.PENDIENTE, false, 0);
	}

	private EntregarAvisosPendientesServicio entrega() {
		return new EntregarAvisosPendientesServicio(avisos, dispositivos, enviador, RELOJ);
	}

	// --- Generar -------------------------------------------------------------------------------------------

	@Test
	void generaUnAvisoPorPendienteYNoCuentaLosQueYaExistian() {
		given(avisos.pendientesPorAvisar(AHORA)).willReturn(List.of(
				new PendientePorAvisar(ESTUDIANTE, 11, "Taller de redes", Instant.parse("2026-10-15T04:59:00Z")),
				new PendientePorAvisar(ESTUDIANTE, 12, "Ensayo", Instant.parse("2026-10-15T13:00:00Z"))));
		given(avisos.crearDeVencimiento(any(Aviso.class), eq(11))).willReturn(true);
		given(avisos.crearDeVencimiento(any(Aviso.class), eq(12))).willReturn(false);

		int creados = new GenerarAvisosDeVencimientoServicio(avisos, new RedactorDeAvisos(), RELOJ).ejecutar();

		assertThat(creados).isEqualTo(1);
		ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
		verify(avisos).crearDeVencimiento(captor.capture(), eq(11));
		assertThat(captor.getValue().tipo()).isEqualTo(TipoDeAviso.VENCIMIENTO_PROXIMO);
		assertThat(captor.getValue().titulo()).isEqualTo("Vence pronto");
		assertThat(captor.getValue().mensaje()).isEqualTo("«Taller de redes» vence hoy a las 11:59 p. m.");
		assertThat(captor.getValue().estadoEnvio()).isEqualTo(EstadoDeEnvio.PENDIENTE);
		assertThat(captor.getValue().leido()).isFalse();
	}

	@Test
	void sinPendientesPorAvisarNoCreaNada() {
		given(avisos.pendientesPorAvisar(AHORA)).willReturn(List.of());

		assertThat(new GenerarAvisosDeVencimientoServicio(avisos, new RedactorDeAvisos(), RELOJ).ejecutar()).isZero();
		verify(avisos, never()).crearDeVencimiento(any(), anyInt());
	}

	// --- Entregar ------------------------------------------------------------------------------------------

	@Test
	void sinWebPushElAvisoQuedaFallidoYSoloSeVeDentroDeLaAplicacion() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(aviso(1, TipoDeAviso.VENCIMIENTO_PROXIMO)));
		given(enviador.disponible()).willReturn(false);

		assertThat(entrega().ejecutar()).isZero();

		verify(avisos).registrarFallo(1, true);
		verifyNoInteractions(dispositivos);
	}

	@Test
	void sinDispositivosConAvisosElAvisoQuedaFallidoPeroNoDesaparece() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(aviso(1, TipoDeAviso.VENCIMIENTO_PROXIMO)));
		given(enviador.disponible()).willReturn(true);
		given(dispositivos.activos(ESTUDIANTE)).willReturn(List.of());

		assertThat(entrega().ejecutar()).isZero();

		verify(avisos).registrarFallo(1, true);
		verify(enviador, never()).enviar(any(), any(), any());
	}

	@Test
	void entregadoAUnDispositivoQuedaEnviadoConElTextoYLaRutaDelPendiente() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(aviso(1, TipoDeAviso.VENCIMIENTO_PROXIMO)));
		given(enviador.disponible()).willReturn(true);
		given(dispositivos.activos(ESTUDIANTE)).willReturn(List.of(dispositivo(1)));
		given(enviador.enviar(any(), any(), any())).willReturn(ResultadoDeEnvioPush.ENTREGADO);

		assertThat(entrega().ejecutar()).isEqualTo(1);

		ArgumentCaptor<CargaDePush> carga = ArgumentCaptor.forClass(CargaDePush.class);
		verify(enviador).enviar(eq(dispositivo(1).suscripcion()), carga.capture(), eq(Duration.ofHours(6)));
		assertThat(carga.getValue().titulo()).isEqualTo("Vence pronto");
		assertThat(carga.getValue().ruta()).isEqualTo("/pendientes");
		verify(avisos).marcarEnviado(1, AHORA);
		verify(avisos, never()).registrarFallo(anyInt(), org.mockito.ArgumentMatchers.anyBoolean());
	}

	@Test
	void unAvisoDeCambioDeRiesgoLlevaALaBrujula() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(aviso(2, TipoDeAviso.CAMBIO_RIESGO)));
		given(enviador.disponible()).willReturn(true);
		given(dispositivos.activos(ESTUDIANTE)).willReturn(List.of(dispositivo(1)));
		given(enviador.enviar(any(), any(), any())).willReturn(ResultadoDeEnvioPush.ENTREGADO);

		entrega().ejecutar();

		ArgumentCaptor<CargaDePush> carga = ArgumentCaptor.forClass(CargaDePush.class);
		verify(enviador).enviar(any(), carga.capture(), any());
		assertThat(carga.getValue().ruta()).isEqualTo("/brujula");
	}

	@Test
	void conUnDispositivoMuertoYOtroVivoSeDesactivaElMuertoYElAvisoQuedaEnviado() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(aviso(1, TipoDeAviso.VENCIMIENTO_PROXIMO)));
		given(enviador.disponible()).willReturn(true);
		given(dispositivos.activos(ESTUDIANTE)).willReturn(List.of(dispositivo(1), dispositivo(2)));
		given(enviador.enviar(eq(dispositivo(1).suscripcion()), any(), any())).willReturn(ResultadoDeEnvioPush.SUSCRIPCION_MUERTA);
		given(enviador.enviar(eq(dispositivo(2).suscripcion()), any(), any())).willReturn(ResultadoDeEnvioPush.ENTREGADO);

		assertThat(entrega().ejecutar()).isEqualTo(1);

		verify(dispositivos).desactivar(ESTUDIANTE, 1);
		verify(dispositivos, never()).desactivar(ESTUDIANTE, 2);
		verify(avisos).marcarEnviado(1, AHORA);
	}

	@Test
	void siTodosFallanDeFormaPasajeraSeReintentaEnLaSiguienteRonda() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(aviso(1, TipoDeAviso.VENCIMIENTO_PROXIMO)));
		given(enviador.disponible()).willReturn(true);
		given(dispositivos.activos(ESTUDIANTE)).willReturn(List.of(dispositivo(1), dispositivo(2)));
		given(enviador.enviar(eq(dispositivo(1).suscripcion()), any(), any())).willReturn(ResultadoDeEnvioPush.FALLO_PASAJERO);
		given(enviador.enviar(eq(dispositivo(2).suscripcion()), any(), any())).willReturn(ResultadoDeEnvioPush.SUSCRIPCION_MUERTA);

		assertThat(entrega().ejecutar()).isZero();

		verify(avisos).registrarFallo(1, false);
		verify(dispositivos).desactivar(ESTUDIANTE, 2);
		verify(avisos, never()).marcarEnviado(anyInt(), any());
	}

	@Test
	void siTodosLosDispositivosEstanMuertosElAvisoQuedaFallidoYNoSeReintenta() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(aviso(1, TipoDeAviso.VENCIMIENTO_PROXIMO)));
		given(enviador.disponible()).willReturn(true);
		given(dispositivos.activos(ESTUDIANTE)).willReturn(List.of(dispositivo(1)));
		given(enviador.enviar(any(), any(), any())).willReturn(ResultadoDeEnvioPush.SUSCRIPCION_MUERTA);

		assertThat(entrega().ejecutar()).isZero();

		verify(dispositivos).desactivar(ESTUDIANTE, 1);
		verify(avisos).registrarFallo(1, true);
	}

	@Test
	void entregaVariosAvisosYCuentaSoloLosQueLlegaron() {
		given(avisos.porEntregar(anyInt())).willReturn(List.of(
				aviso(1, TipoDeAviso.VENCIMIENTO_PROXIMO), aviso(2, TipoDeAviso.VENCIMIENTO_PROXIMO)));
		given(enviador.disponible()).willReturn(true);
		given(dispositivos.activos(ESTUDIANTE)).willReturn(List.of(dispositivo(1)));
		given(enviador.enviar(any(), any(), any()))
				.willReturn(ResultadoDeEnvioPush.ENTREGADO, ResultadoDeEnvioPush.FALLO_PASAJERO);

		assertThat(entrega().ejecutar()).isEqualTo(1);

		verify(avisos).marcarEnviado(1, AHORA);
		verify(avisos).registrarFallo(2, false);
	}

	// --- Listar y marcar -------------------------------------------------------------------------------------

	@Test
	void listaMisAvisosConCuantosNoHeLeido() {
		given(avisos.sinLeer(ESTUDIANTE)).willReturn(3);
		given(avisos.deEstudiante(eq(ESTUDIANTE), eq(true), anyInt())).willReturn(List.of(aviso(1, TipoDeAviso.CAMBIO_RIESGO)));

		var resultado = new ListarMisAvisosServicio(avisos).ejecutar(ESTUDIANTE, true);

		assertThat(resultado.sinLeer()).isEqualTo(3);
		assertThat(resultado.avisos()).hasSize(1);
	}

	@Test
	void marcarUnAvisoQueNoEsMioRespondeComoSiNoExistiera() {
		given(avisos.marcarLeido(ESTUDIANTE, 99, true)).willReturn(false);

		assertThatThrownBy(() -> new MarcarMiAvisoComoLeidoServicio(avisos).ejecutar(ESTUDIANTE, 99, true))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("No encontramos ese aviso");
	}

	@Test
	void marcarUnAvisoMioFunciona() {
		given(avisos.marcarLeido(ESTUDIANTE, 1, false)).willReturn(true);

		new MarcarMiAvisoComoLeidoServicio(avisos).ejecutar(ESTUDIANTE, 1, false);

		verify(avisos).marcarLeido(ESTUDIANTE, 1, false);
	}

	@Test
	void marcarTodosComoLeidosLoPideAlPuerto() {
		new MarcarMisAvisosComoLeidosServicio(avisos).ejecutar(ESTUDIANTE);

		verify(avisos).marcarTodosLeidos(ESTUDIANTE);
	}

	// --- Suscripción ---------------------------------------------------------------------------------------

	@Test
	void guardaLaSuscripcionConElNombreDelDispositivo() {
		SuscripcionPush suscripcion = suscripcion("a");
		DispositivoConAvisos guardado = new DispositivoConAvisos(1, "Chrome en Android", suscripcion, true, AHORA);
		given(dispositivos.guardarSuscripcion(ESTUDIANTE, "Chrome en Android", suscripcion, AHORA)).willReturn(guardado);

		assertThat(new RegistrarMiSuscripcionPushServicio(dispositivos, RELOJ).ejecutar(ESTUDIANTE, "  Chrome en Android ", suscripcion))
				.isSameAs(guardado);
	}

	@Test
	void sinNombreUsaEsteDispositivoYUnNombreLargoSeRecorta() {
		SuscripcionPush suscripcion = suscripcion("a");
		var servicio = new RegistrarMiSuscripcionPushServicio(dispositivos, RELOJ);

		servicio.ejecutar(ESTUDIANTE, "   ", suscripcion);
		servicio.ejecutar(ESTUDIANTE, null, suscripcion);
		servicio.ejecutar(ESTUDIANTE, "x".repeat(200), suscripcion);

		verify(dispositivos, org.mockito.Mockito.times(2)).guardarSuscripcion(ESTUDIANTE, "Este dispositivo", suscripcion, AHORA);
		verify(dispositivos).guardarSuscripcion(ESTUDIANTE, "x".repeat(80), suscripcion, AHORA);
	}

	@Test
	void conDiezDispositivosNoAdmiteUnoNuevoPeroSiVolverASuscribirUnoQueYaEsta() {
		List<DispositivoConAvisos> diez = java.util.stream.IntStream.rangeClosed(1, 10).mapToObj(AvisosServiciosTest::dispositivo).toList();
		given(dispositivos.activos(ESTUDIANTE)).willReturn(diez);
		var servicio = new RegistrarMiSuscripcionPushServicio(dispositivos, RELOJ);

		assertThatThrownBy(() -> servicio.ejecutar(ESTUDIANTE, "Nuevo", suscripcion("nuevo")))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("10 dispositivos");
		verify(dispositivos, never()).guardarSuscripcion(anyInt(), any(), any(), any());

		servicio.ejecutar(ESTUDIANTE, "Ya estaba", suscripcion("d3"));

		verify(dispositivos).guardarSuscripcion(ESTUDIANTE, "Ya estaba", suscripcion("d3"), AHORA);
	}

	@Test
	void sinSuscripcionSeRechaza() {
		assertThatThrownBy(() -> new RegistrarMiSuscripcionPushServicio(dispositivos, RELOJ).ejecutar(ESTUDIANTE, "x", null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		verifyNoInteractions(dispositivos);
	}

	@Test
	void laLlavePublicaSaleDelEnviador() {
		given(enviador.llavePublica()).willReturn(Optional.of("llave"));

		assertThat(new ConsultarLlaveDePushServicio(enviador).ejecutar()).contains("llave");
	}
}
