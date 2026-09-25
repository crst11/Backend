package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EmisorDeTokensPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EmisorDeTokensPort.TokenDeAcceso;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.LimitadorDeIntentosPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.CredencialesInvalidasException;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.DemasiadosIntentosException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Sesion;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IniciarSesionServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");
	private static final String CORREO = "ana.diaz@ucundinamarca.edu.co";
	private static final OrigenDeSesion ORIGEN = new OrigenDeSesion("10.0.0.1", "Firefox");

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final CifradorDeContrasenaPort cifrador = mock(CifradorDeContrasenaPort.class);
	private final EmisorDeTokensPort tokens = mock(EmisorDeTokensPort.class);
	private final LimitadorDeIntentosPort limitador = mock(LimitadorDeIntentosPort.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private IniciarSesionServicio servicio;

	private Estudiante cuenta(EstadoCuenta estado) {
		return new Estudiante(1, "Ana", "Díaz", new CorreoInstitucional(CORREO), estado, true, AHORA);
	}

	@BeforeEach
	void configurar() {
		servicio = new IniciarSesionServicio(
				estudiantes, sesiones, cifrador, tokens, limitador, reloj, Duration.ofMinutes(20), Duration.ofDays(7));
		given(reloj.ahora()).willReturn(AHORA);
		given(estudiantes.buscarPorCorreo(new CorreoInstitucional(CORREO))).willReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA)));
		given(estudiantes.contrasenaCifradaDe(1)).willReturn(Optional.of("hash"));
		given(cifrador.coincide("claveSegura1", "hash")).willReturn(true);
		given(tokens.generarTokenDeRefresco()).willReturn("refresco-123");
		given(tokens.emitirAcceso(any(), any(), any())).willReturn(new TokenDeAcceso("jwt", AHORA.plus(Duration.ofMinutes(20))));
	}

	@Test
	void iniciaSesionYGuardaSoloLaHuellaDelRefresco() {
		var sesion = servicio.ejecutar(CORREO, "claveSegura1", ORIGEN);

		assertThat(sesion.tokenDeAcceso()).isEqualTo("jwt");
		assertThat(sesion.tokenDeRefresco()).isEqualTo("refresco-123");
		assertThat(sesion.refrescoExpira()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
		var guardada = ArgumentCaptor.forClass(Sesion.class);
		verify(sesiones).guardar(guardada.capture());
		assertThat(guardada.getValue().huellaRefresco()).isEqualTo(Sesion.huellaDe("refresco-123"));
		assertThat(guardada.getValue().ipOrigen()).isEqualTo("10.0.0.1");
		verify(estudiantes).registrarUltimoAcceso(1, AHORA);
		verify(limitador).reiniciar("correo:" + CORREO);
	}

	@Test
	void unaContrasenaIncorrectaSumaFalloPorCorreoYPorIp() {
		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "otraClave", ORIGEN))
				.isInstanceOf(CredencialesInvalidasException.class);

		verify(limitador).registrarFallo("correo:" + CORREO);
		verify(limitador).registrarFallo("ip:10.0.0.1");
		verify(sesiones, never()).guardar(any());
	}

	@Test
	void unCorreoInexistenteRecibeElMismoMensajeQueUnaContrasenaIncorrecta() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.empty());
		String mensajeInexistente = mensajeDe(() -> servicio.ejecutar("nadie@ucundinamarca.edu.co", "claveSegura1", ORIGEN));
		String mensajeIncorrecta = mensajeDe(() -> servicio.ejecutar(CORREO, "otraClave", ORIGEN));

		assertThat(mensajeInexistente).isEqualTo(mensajeIncorrecta);
		verify(cifrador).cifrar("claveSegura1");
	}

	@Test
	void unCorreoQueNoEsInstitucionalTambienRecibeElMensajeGenerico() {
		assertThatThrownBy(() -> servicio.ejecutar("ana@gmail.com", "claveSegura1", ORIGEN))
				.isInstanceOf(CredencialesInvalidasException.class);
		verify(limitador).registrarFallo("correo:ana@gmail.com");
	}

	@Test
	void unCorreoBloqueadoNiSiquieraConsultaLaCuenta() {
		given(limitador.estaBloqueado("correo:" + CORREO)).willReturn(true);

		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "claveSegura1", ORIGEN))
				.isInstanceOf(DemasiadosIntentosException.class);
		verify(estudiantes, never()).buscarPorCorreo(any());
	}

	@Test
	void unaIpBloqueadaTambienSeRechaza() {
		given(limitador.estaBloqueado("ip:10.0.0.1")).willReturn(true);

		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "claveSegura1", ORIGEN))
				.isInstanceOf(DemasiadosIntentosException.class);
	}

	@Test
	void unaCuentaPendienteNoInicia() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(cuenta(EstadoCuenta.PENDIENTE)));

		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "claveSegura1", ORIGEN))
				.isInstanceOf(CuentaNoActivaException.class)
				.hasMessageContaining("Verifica tu correo");
		verify(sesiones, never()).guardar(any());
	}

	@Test
	void unaCuentaInactivaNoInicia() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(cuenta(EstadoCuenta.INACTIVA)));

		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "claveSegura1", ORIGEN))
				.isInstanceOf(CuentaNoActivaException.class)
				.hasMessageContaining("inactiva");
	}

	private static String mensajeDe(Runnable accion) {
		try {
			accion.run();
			return "no falló";
		} catch (CredencialesInvalidasException e) {
			return e.getMessage();
		}
	}
}
