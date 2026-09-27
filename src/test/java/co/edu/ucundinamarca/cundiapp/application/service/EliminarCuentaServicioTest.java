package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EliminarCuentaServicioTest {

	private static final Instant AHORA = Instant.parse("2026-01-15T10:00:00Z");

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final SesionRepositorio sesiones = mock(SesionRepositorio.class);
	private final RelojPort reloj = mock(RelojPort.class);
	private EliminarCuentaServicio servicio;

	@BeforeEach
	void configurar() {
		servicio = new EliminarCuentaServicio(estudiantes, sesiones, reloj);
		given(reloj.ahora()).willReturn(AHORA);
	}

	@Test
	void dejaLaCuentaInactivaYRevocaTodasSusSesiones() {
		var activa = new Estudiante(7, "Ana", "Díaz", new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co"),
				EstadoCuenta.ACTIVA, true, Instant.now());
		given(estudiantes.buscarPorId(7)).willReturn(Optional.of(activa));

		servicio.ejecutar(7);

		verify(estudiantes).guardarDesactivacion(activa.desactivar());
		verify(sesiones).revocarVigentes(eq(7), eq(MotivoDeRevocacion.CIERRE_SESION), eq(AHORA));
	}

	@Test
	void siLaCuentaYaNoExisteLaSesionNoSirve() {
		given(estudiantes.buscarPorId(9)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.ejecutar(9)).isInstanceOf(SesionInvalidaException.class);
	}
}
