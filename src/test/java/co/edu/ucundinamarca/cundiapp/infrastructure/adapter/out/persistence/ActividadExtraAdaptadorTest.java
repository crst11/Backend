package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.ActividadExtraRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadExtra;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Las actividades extra contra PostgreSQL real (Testcontainers) (SCRUM-266). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ActividadExtraAdaptadorTest {

	private static final int SIN_LIMITE_PRACTICO = 100;

	@Autowired
	private ActividadExtraRepositorio actividades;

	@Autowired
	private HorarioRepositorio horario;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private JdbcTemplate jdbc;

	private int nuevoEstudiante() {
		var correo = new CorreoInstitucional("extra." + UUID.randomUUID().toString().substring(0, 8) + "@ucundinamarca.edu.co");
		return estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Prueba", "Extra", correo, EstadoCuenta.ACTIVA, true, Instant.now()), "hash-de-prueba").id();
	}

	private static ActividadExtra baile(int idEstudiante, DiaDeLaSemana dia) {
		return ActividadExtra.nueva(idEstudiante, "Baile", dia, LocalTime.of(17, 0), LocalTime.of(19, 0), "Coliseo", "Profe Ana");
	}

	@Test
	void guardaUnaFilaPorDiaYLasDevuelveConSuNumero() {
		int estudiante = nuevoEstudiante();

		var guardadas = actividades.guardarSiCaben(
				estudiante, List.of(baile(estudiante, DiaDeLaSemana.LUNES), baile(estudiante, DiaDeLaSemana.MIERCOLES)), 20)
				.orElseThrow();

		assertThat(guardadas).hasSize(2).allSatisfy(a -> assertThat(a.id()).isNotNull());
		assertThat(actividades.de(estudiante)).extracting(ActividadExtra::dia)
				.containsExactlyInAnyOrder(DiaDeLaSemana.LUNES, DiaDeLaSemana.MIERCOLES);
	}

	@Test
	void laLecturaDelHorarioLasSumaALasClasesSinSerDeNingunaAsignatura() {
		int estudiante = nuevoEstudiante();
		actividades.guardarSiCaben(estudiante, List.of(baile(estudiante, DiaDeLaSemana.MARTES)), 20).orElseThrow();

		List<ClaseDelHorario> clases = horario.clasesDe(estudiante);

		assertThat(clases).hasSize(1);
		var extra = clases.get(0);
		assertThat(extra.esExtra()).isTrue();
		assertThat(extra.idMatricula()).isNull();
		assertThat(extra.asignatura()).isEqualTo("Baile");
		assertThat(extra.extra().lugar()).isEqualTo("Coliseo");
		assertThat(extra.extra().responsable()).isEqualTo("Profe Ana");
		assertThat(extra.bloque().horaInicio()).isEqualTo(LocalTime.of(17, 0));
	}

	@Test
	void noGuardaNadaSiNoCabenTodas() {
		int estudiante = nuevoEstudiante();
		actividades.guardarSiCaben(estudiante, List.of(baile(estudiante, DiaDeLaSemana.LUNES)), 3).orElseThrow();

		var resultado = actividades.guardarSiCaben(
				estudiante, List.of(baile(estudiante, DiaDeLaSemana.MARTES), baile(estudiante, DiaDeLaSemana.JUEVES),
						baile(estudiante, DiaDeLaSemana.VIERNES)), 3);

		assertThat(resultado).isEmpty();
		assertThat(actividades.de(estudiante)).hasSize(1);
	}

	@Test
	void dosPeticionesALaVezNoPasanDelLimite() throws Exception {
		int estudiante = nuevoEstudiante();
		var seisDias = Arrays.stream(DiaDeLaSemana.values()).limit(6).toList();
		var inicio = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			// Cada petición trae doce, y el límite es veinte: las dos no caben, y una sola sí.
			Callable<Boolean> peticion = () -> {
				inicio.await();
				var lista = new ArrayList<ActividadExtra>();
				for (int vuelta = 0; vuelta < 2; vuelta++) {
					seisDias.forEach(dia -> lista.add(baile(estudiante, dia)));
				}
				return actividades.guardarSiCaben(estudiante, lista, 20).isPresent();
			};
			Future<Boolean> primera = pool.submit(peticion);
			Future<Boolean> segunda = pool.submit(peticion);
			inicio.countDown();

			assertThat(List.of(primera.get(), segunda.get())).containsExactlyInAnyOrder(true, false);
			assertThat(actividades.de(estudiante)).hasSize(12);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void soloQuitaLasDelDuenoYDiceSiExistia() {
		int dueno = nuevoEstudiante();
		int otro = nuevoEstudiante();
		int id = actividades.guardarSiCaben(dueno, List.of(baile(dueno, DiaDeLaSemana.LUNES)), SIN_LIMITE_PRACTICO)
				.orElseThrow().get(0).id();

		assertThat(actividades.quitar(otro, id)).isFalse();
		assertThat(actividades.de(dueno)).hasSize(1);

		assertThat(actividades.quitar(dueno, id)).isTrue();
		assertThat(actividades.de(dueno)).isEmpty();
		assertThat(actividades.quitar(dueno, id)).isFalse();
	}

	@Test
	void alBorrarAlEstudianteSeBorranSusActividades() {
		int estudiante = nuevoEstudiante();
		actividades.guardarSiCaben(estudiante, List.of(baile(estudiante, DiaDeLaSemana.SABADO)), 20).orElseThrow();

		jdbc.update("DELETE FROM estudiante WHERE id_estudiante = ?", estudiante);

		assertThat(jdbc.queryForObject(
				"SELECT COUNT(*) FROM actividad_extra WHERE id_estudiante = ?", Integer.class, estudiante)).isZero();
	}
}
