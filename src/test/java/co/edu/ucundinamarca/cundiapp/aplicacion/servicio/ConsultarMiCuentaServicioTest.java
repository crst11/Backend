package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarMiCuentaServicioTest {

	private final EstudianteRepositorio estudiantes = mock(EstudianteRepositorio.class);
	private final ConsultarMiCuentaServicio servicio = new ConsultarMiCuentaServicio(estudiantes);

	@Test
	void devuelveLaCuentaDelEstudianteDelToken() {
		var cuenta = new Estudiante(7, "Ana", "Díaz", new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co"),
				EstadoCuenta.ACTIVA, true, Instant.now());
		given(estudiantes.buscarPorId(7)).willReturn(Optional.of(cuenta));

		assertThat(servicio.ejecutar(7)).isEqualTo(cuenta);
	}

	@Test
	void siLaCuentaYaNoExisteLaSesionNoSirve() {
		given(estudiantes.buscarPorId(9)).willReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.ejecutar(9)).isInstanceOf(SesionInvalidaException.class);
	}
}
