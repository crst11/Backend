package co.edu.ucundinamarca.cundiapp.infraestructura.salida.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
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

		assertThat(repositorio.existeCuentaCon(correo)).isFalse();

		Estudiante guardado = repositorio.guardarConCredencialLocal(estudiante, "hash-de-prueba");

		assertThat(guardado.id()).isNotNull();
		assertThat(guardado.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
		assertThat(repositorio.existeCuentaCon(correo)).isTrue();
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
}
