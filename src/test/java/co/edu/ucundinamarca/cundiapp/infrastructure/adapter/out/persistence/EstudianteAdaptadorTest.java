package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Prueba de integración contra PostgreSQL real (Testcontainers): guarda cuenta y credencial. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EstudianteAdaptadorTest {

	@Autowired
	private EstudianteRepositorio repositorio;

	@Test
	void guardaLaCuentaYLaCredencialLocalYLasPuedeVolverAEncontrar() {
		var correo = new CorreoInstitucional("integracion.test@ucundinamarca.edu.co");
		var estudiante = new Estudiante(
				null, "Prueba", "Integración", correo, EstadoCuenta.PENDIENTE, true, Instant.now());

		assertThat(repositorio.buscarPorCorreo(correo)).isEmpty();

		Estudiante guardado = repositorio.guardarConCredencialLocal(estudiante, "hash-de-prueba");

		assertThat(guardado.id()).isNotNull();
		assertThat(guardado.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		assertThat(repositorio.buscarPorCorreo(correo)).isPresent();
	}

	@Test
	void buscaPorCorreoSinDistinguirMayusculasYActivaLaCuenta() {
		var correo = new CorreoInstitucional("activacion.test@ucundinamarca.edu.co");
		var guardado = repositorio.guardarConCredencialLocal(
				new Estudiante(null, "Prueba", "Activación", correo, EstadoCuenta.PENDIENTE, true, Instant.now()),
				"hash-de-prueba");

		var encontrado = repositorio.buscarPorCorreo(new CorreoInstitucional("ACTIVACION.test@ucundinamarca.edu.co"));
		assertThat(encontrado).isPresent();
		assertThat(encontrado.get().estaPendiente()).isTrue();

		repositorio.guardarActivacion(guardado.activar());

		assertThat(repositorio.buscarPorCorreo(correo).orElseThrow().estado()).isEqualTo(EstadoCuenta.ACTIVA);
	}

	@Test
	void eliminaLaCuentaYQuedaInactivaSinPerderSusDatos() {
		var correo = new CorreoInstitucional("eliminacion.test@ucundinamarca.edu.co");
		var guardado = repositorio.guardarConCredencialLocal(
				new Estudiante(null, "Prueba", "Eliminación", correo, EstadoCuenta.PENDIENTE, true, Instant.now()),
				"hash-de-prueba");
		repositorio.guardarActivacion(guardado.activar());
		var activo = repositorio.buscarPorId(guardado.id()).orElseThrow();

		repositorio.guardarDesactivacion(activo.desactivar());

		var eliminado = repositorio.buscarPorId(guardado.id()).orElseThrow();
		assertThat(eliminado.estado()).isEqualTo(EstadoCuenta.INACTIVA);
		assertThat(eliminado.nombres()).isEqualTo("Prueba");
		assertThat(eliminado.correo()).isEqualTo(correo);
	}

	@Test
	void registrarseDeNuevoSobreUnaCuentaEliminadaReescribeLaMismaFilaYPideVerificarOtraVez() {
		var correo = new CorreoInstitucional("reactivacion.test@ucundinamarca.edu.co");
		var primeraVez = repositorio.guardarConCredencialLocal(
				new Estudiante(null, "Prueba Vieja", "Uno", correo, EstadoCuenta.PENDIENTE, true, Instant.now()),
				"hash-viejo");
		repositorio.guardarActivacion(primeraVez.activar());
		repositorio.guardarDesactivacion(repositorio.buscarPorId(primeraVez.id()).orElseThrow().desactivar());

		var paraReactivar = new Estudiante(
				primeraVez.id(), "Prueba Nueva", "Dos", correo, EstadoCuenta.PENDIENTE, true, Instant.now());
		Estudiante reactivado = repositorio.reactivarConCredencialLocal(paraReactivar, "hash-nuevo");

		assertThat(reactivado.id()).isEqualTo(primeraVez.id());
		assertThat(reactivado.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		assertThat(reactivado.nombres()).isEqualTo("Prueba Nueva");
		assertThat(repositorio.contrasenaCifradaDe(primeraVez.id())).contains("hash-nuevo");
		// El correo se debe volver a verificar: la activación anterior ya no cuenta.
		var recienActivado = repositorio.buscarPorId(primeraVez.id()).map(Estudiante::estaPendiente);
		assertThat(recienActivado).contains(true);
	}
}
