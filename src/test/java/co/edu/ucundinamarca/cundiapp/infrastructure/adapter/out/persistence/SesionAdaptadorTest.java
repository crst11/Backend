package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Contra PostgreSQL real: consecutivos por estudiante, rotación y revocación en bloque. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SesionAdaptadorTest {

	@Autowired
	private SesionRepositorio sesiones;

	@Autowired
	private EstudianteRepositorio estudiantes;

	private int nuevoEstudiante(String correo) {
		return estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Prueba", "Sesión", new CorreoInstitucional(correo), EstadoCuenta.ACTIVA, true, Instant.now()),
				"hash-de-prueba").id();
	}

	private Sesion abrir(int idEstudiante, String token, Instant ahora) {
		return Sesion.abrir(idEstudiante, token, ahora, Duration.ofDays(7), "Firefox", "10.0.0.1");
	}

	@Test
	void guardaYVuelveAEncontrarLaSesionPorLaHuellaDelToken() {
		int id = nuevoEstudiante("sesion.guardar@ucundinamarca.edu.co");
		Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		sesiones.guardar(abrir(id, "token-a", ahora));

		var encontrada = sesiones.buscarPorHuella(Sesion.huellaDe("token-a")).orElseThrow();
		assertThat(encontrada.idEstudiante()).isEqualTo(id);
		assertThat(encontrada.consecutivo()).isEqualTo(1);
		assertThat(encontrada.estaVigente(ahora.plusSeconds(1))).isTrue();
		assertThat(encontrada.userAgent()).isEqualTo("Firefox");
		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe("otro"))).isEmpty();
	}

	@Test
	void elConsecutivoAvanzaPorEstudiante() {
		int uno = nuevoEstudiante("sesion.consec1@ucundinamarca.edu.co");
		int dos = nuevoEstudiante("sesion.consec2@ucundinamarca.edu.co");
		Instant ahora = Instant.now();

		sesiones.guardar(abrir(uno, "c1-a", ahora));
		sesiones.guardar(abrir(uno, "c1-b", ahora));
		sesiones.guardar(abrir(dos, "c2-a", ahora));

		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe("c1-b")).orElseThrow().consecutivo()).isEqualTo(2);
		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe("c2-a")).orElseThrow().consecutivo()).isEqualTo(1);
	}

	@Test
	void rotarRevocaLaUsadaYAbreLaNueva() {
		int id = nuevoEstudiante("sesion.rotar@ucundinamarca.edu.co");
		Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		sesiones.guardar(abrir(id, "rot-vieja", ahora));
		var usada = sesiones.buscarPorHuella(Sesion.huellaDe("rot-vieja")).orElseThrow();

		sesiones.rotar(usada.revocar(MotivoDeRevocacion.ROTACION, ahora), abrir(id, "rot-nueva", ahora));

		var vieja = sesiones.buscarPorHuella(Sesion.huellaDe("rot-vieja")).orElseThrow();
		assertThat(vieja.fueRotada()).isTrue();
		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe("rot-nueva")).orElseThrow().consecutivo()).isEqualTo(2);
	}

	@Test
	void revocarVigentesCierraSoloLasQueSiguenAbiertas() {
		int id = nuevoEstudiante("sesion.revocar@ucundinamarca.edu.co");
		Instant ahora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		sesiones.guardar(abrir(id, "rev-a", ahora));
		sesiones.guardar(abrir(id, "rev-b", ahora));
		var cerrada = sesiones.buscarPorHuella(Sesion.huellaDe("rev-a")).orElseThrow();
		sesiones.actualizar(cerrada.revocar(MotivoDeRevocacion.CIERRE_SESION, ahora));

		sesiones.revocarVigentes(id, MotivoDeRevocacion.REUSO_DETECTADO, ahora.plusSeconds(5));

		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe("rev-a")).orElseThrow().motivoRevocacion())
				.isEqualTo(MotivoDeRevocacion.CIERRE_SESION);
		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe("rev-b")).orElseThrow().motivoRevocacion())
				.isEqualTo(MotivoDeRevocacion.REUSO_DETECTADO);
	}
}
