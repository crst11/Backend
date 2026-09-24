package co.edu.ucundinamarca.cundiapp.dominio.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EstudianteTest {

	private static final CorreoInstitucional CORREO = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");

	@Test
	void naceEnEstadoPendienteConSusDatos() {
		var estudiante = new Estudiante(null, "Ana", "Díaz", CORREO, EstadoCuenta.PENDIENTE, true, Instant.now());

		assertThat(estudiante.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		assertThat(estudiante.consentimientoDatos()).isTrue();
	}

	@Test
	void seActivaAlVerificarElCorreo() {
		var pendiente = new Estudiante(1, "Ana", "Díaz", CORREO, EstadoCuenta.PENDIENTE, true, Instant.now());

		assertThat(pendiente.estaPendiente()).isTrue();
		assertThat(pendiente.activar().estado()).isEqualTo(EstadoCuenta.ACTIVA);
	}

	@Test
	void unaCuentaInactivaNoSeActivaConUnCodigo() {
		var inactiva = new Estudiante(1, "Ana", "Díaz", CORREO, EstadoCuenta.INACTIVA, true, Instant.now());

		assertThatThrownBy(inactiva::activar).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void rechazaRegistrarseSinAceptarElTratamientoDeDatos() {
		assertThatThrownBy(() -> new Estudiante(null, "Ana", "Díaz", CORREO, EstadoCuenta.PENDIENTE, false, Instant.now()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("tratamiento de datos");
	}

	@Test
	void rechazaUnConsentimientoSinFecha() {
		assertThatThrownBy(() -> new Estudiante(null, "Ana", "Díaz", CORREO, EstadoCuenta.PENDIENTE, true, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void rechazaNombresOApellidosEnBlanco() {
		assertThatThrownBy(() -> new Estudiante(null, " ", "Díaz", CORREO, EstadoCuenta.PENDIENTE, true, Instant.now()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new Estudiante(null, "Ana", " ", CORREO, EstadoCuenta.PENDIENTE, true, Instant.now()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}
}
