package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

class VerificarCorreoServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");
	private static final String CORREO = "ana.diaz@ucundinamarca.edu.co";

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final CodigoDeVerificacionRepositorio codigos = mock(CodigoDeVerificacionRepositorio.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private VerificarCorreoServicio servicio;

	private final Estudiante pendiente = new Estudiante(
			1, "Ana", "Díaz", new CorreoInstitucional(CORREO), EstadoCuenta.PENDIENTE, true, AHORA);

	@BeforeEach
	void configurar() {
		servicio = new VerificarCorreoServicio(estudiantes, codigos, reloj);
		given(reloj.ahora()).willReturn(AHORA.plusSeconds(60));
		given(estudiantes.buscarPorCorreo(new CorreoInstitucional(CORREO))).willReturn(Optional.of(pendiente));
		given(codigos.buscarDe(1)).willReturn(Optional.of(CodigoDeVerificacion.emitir(1, "123456", AHORA)));
	}

	@Test
	void activaLaCuentaConElCodigoCorrecto() {
		Estudiante resultado = servicio.ejecutar(CORREO, "123456");

		assertThat(resultado.estado()).isEqualTo(EstadoCuenta.ACTIVA);
		verify(estudiantes).guardarActivacion(resultado);
	}

	@Test
	void guardaElIntentoFallidoYNoActivaLaCuenta() {
		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "000000"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Te quedan 4 intentos");

		verify(codigos).guardar(any());
		verify(estudiantes, never()).guardarActivacion(any());
	}

	@Test
	void rechazaUnCodigoVencido() {
		given(reloj.ahora()).willReturn(AHORA.plusSeconds(15 * 60 + 1));

		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "123456"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("venció");
		verify(estudiantes, never()).guardarActivacion(any());
	}

	@Test
	void unCorreoSinCuentaRecibeElMensajeGenerico() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "123456"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("incorrecto o venció");
	}

	@Test
	void unaCuentaYaActivaNoSeVerificaDeNuevo() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.of(pendiente.activar()));

		assertThatThrownBy(() -> servicio.ejecutar(CORREO, "123456"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("ya está verificada");
	}
}
