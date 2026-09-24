package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CodigoDeVerificacionRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.RelojPort;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
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
		given(codigos.buscarDe(1)).willReturn(Optional.of(CodigoDeVerificacion.emitir(1, "123456", EMISION)));
		given(reloj.ahora()).willReturn(EMISION.plusSeconds(16 * 60));

		servicio.ejecutar(CORREO);

		verify(emisor).emitirYEnviar(pendiente);
	}

	@Test
	void emiteUnCodigoSiNuncaSeHabiaEmitido() {
		given(codigos.buscarDe(1)).willReturn(Optional.empty());
		given(reloj.ahora()).willReturn(EMISION);

		servicio.ejecutar(CORREO);

		verify(emisor).emitirYEnviar(pendiente);
	}

	@Test
	void rechazaPedirOtroMientrasElActualSigueVigente() {
		given(codigos.buscarDe(1)).willReturn(Optional.of(CodigoDeVerificacion.emitir(1, "123456", EMISION)));
		given(reloj.ahora()).willReturn(EMISION.plusSeconds(60));

		assertThatThrownBy(() -> servicio.ejecutar(CORREO))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("sigue vigente");
		verify(emisor, never()).emitirYEnviar(any());
	}

	@Test
	void noHaceNadaNiRevelaSiElCorreoNoTieneCuentaPendiente() {
		given(estudiantes.buscarPorCorreo(any())).willReturn(Optional.empty());

		servicio.ejecutar(CORREO);

		verify(emisor, never()).emitirYEnviar(any());
	}
}
