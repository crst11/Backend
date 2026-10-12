package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.DatosDeRestablecimiento;
import co.edu.ucundinamarca.cundiapp.application.port.out.CifradorDeContrasenaPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestablecerContrasenaServicioTest {

	private static final String CORREO = "ana.diaz@ucundinamarca.edu.co";
	private static final String NUEVA = "ClaveNueva2026!";
	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final CodigoDeVerificacionRepositorio codigos = mock(CodigoDeVerificacionRepositorio.class);
	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final CifradorDeContrasenaPort cifrador = mock(CifradorDeContrasenaPort.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private RestablecerContrasenaServicio servicio;

	private final Estudiante activa = new Estudiante(
			1, "Ana", "Díaz", new CorreoInstitucional(CORREO), EstadoCuenta.ACTIVA, true, AHORA);

	@BeforeEach
	void configurar() {
		servicio = new RestablecerContrasenaServicio(estudiantes, codigos, sesiones, cifrador, reloj);
		given(reloj.ahora()).willReturn(AHORA);
	}

	private void cuentaConCodigo(String codigoEnClaro) {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(activa));
		given(estudiantes.contrasenaCifradaDe(1)).willReturn(Optional.of("hash-anterior"));
		given(codigos.buscarDe(1, PropositoDelCodigo.RECUPERAR_CONTRASENA)).willReturn(Optional.of(
				CodigoDeVerificacion.emitir(1, PropositoDelCodigo.RECUPERAR_CONTRASENA, codigoEnClaro, AHORA)));
	}

	@Test
	void cambiaLaContrasenaYRevocaTodasLasSesiones() {
		cuentaConCodigo("123456");
		given(cifrador.cifrar(NUEVA)).willReturn("hash-nuevo");

		servicio.ejecutar(new DatosDeRestablecimiento(CORREO, "123456", NUEVA));

		verify(estudiantes).cambiarContrasenaLocal(1, "hash-nuevo");
		// Quien recupera su contraseña suele sospechar que alguien más entró: esas sesiones deben caer.
		verify(sesiones).revocarVigentes(eq(1), any(), eq(AHORA));
	}

	@Test
	void rechazaUnaContrasenaNuevaQueNoCumpleLaPolitica() {
		assertThatThrownBy(() -> servicio.ejecutar(new DatosDeRestablecimiento(CORREO, "123456", "clave123")))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(estudiantes, never()).cambiarContrasenaLocal(anyInt(), any());
	}

	@Test
	void rechazaUnCodigoIncorrectoYDescuentaElIntento() {
		cuentaConCodigo("123456");

		assertThatThrownBy(() -> servicio.ejecutar(new DatosDeRestablecimiento(CORREO, "999999", NUEVA)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Te quedan 4 intentos");

		verify(codigos).guardar(any());
		verify(estudiantes, never()).cambiarContrasenaLocal(anyInt(), any());
	}

	@Test
	void rechazaLaContrasenaNuevaSiEsIgualALaAnterior() {
		cuentaConCodigo("123456");
		given(cifrador.coincide(NUEVA, "hash-anterior")).willReturn(true);

		assertThatThrownBy(() -> servicio.ejecutar(new DatosDeRestablecimiento(CORREO, "123456", NUEVA)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("distinta de la anterior");

		verify(estudiantes, never()).cambiarContrasenaLocal(anyInt(), any());
	}

	@Test
	void sinCodigoPedidoNoSePuedeCambiarLaContrasena() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(activa));
		given(estudiantes.contrasenaCifradaDe(1)).willReturn(Optional.of("hash-anterior"));
		given(codigos.buscarDe(1, PropositoDelCodigo.RECUPERAR_CONTRASENA)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.ejecutar(new DatosDeRestablecimiento(CORREO, "123456", NUEVA)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);

		verify(estudiantes, never()).cambiarContrasenaLocal(anyInt(), any());
	}

	@Test
	void unaCuentaQueNoExisteRecibeElMismoRechazoQueUnCodigoVencido() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.ejecutar(new DatosDeRestablecimiento(CORREO, "123456", NUEVA)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("El código no es válido o ya venció");
	}

	private static int anyInt() {
		return org.mockito.ArgumentMatchers.anyInt();
	}
}
