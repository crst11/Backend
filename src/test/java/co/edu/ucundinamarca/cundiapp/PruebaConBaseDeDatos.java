package co.edu.ucundinamarca.cundiapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de las pruebas que se ejecutan contra un PostgreSQL real (Testcontainers) con las
 * migraciones de Flyway aplicadas. Cada prueba corre en una transacción que se revierte.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
abstract class PruebaConBaseDeDatos {

	private static final String HASH_BCRYPT = "$2a$12$" + "x".repeat(53);

	@Autowired
	protected JdbcTemplate jdbc;

	/** Las reglas diferidas (sumas de 100 %, método de acceso activo) se comprueban al confirmar la transacción. */
	protected void confirmar() {
		jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
	}

	/**
	 * La acción debe ser rechazada por la base de datos y el mensaje debe nombrar la regla que la rechazó.
	 * Tras un error PostgreSQL deja la transacción abortada, por eso cada rechazo se aísla con un punto de
	 * recuperación y así una prueba puede comprobar varios rechazos seguidos.
	 */
	protected void rechaza(String reglaEsperada, Runnable accion) {
		jdbc.execute("SAVEPOINT antes_del_rechazo");
		assertThatThrownBy(accion::run)
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining(reglaEsperada);
		jdbc.execute("ROLLBACK TO SAVEPOINT antes_del_rechazo");
	}

	protected void catalogoBasico() {
		jdbc.update("INSERT INTO programa_academico VALUES ('IS','Ingeniería de Sistemas','Ingeniería','Fusagasugá',160,10)");
		jdbc.update("INSERT INTO plan_de_estudios VALUES ('PL1','IS','2020',2020,'vigente')");
		jdbc.update("""
				INSERT INTO asignatura VALUES
				  ('A1','PL1','Cálculo',3,'obligatoria',1),
				  ('A2','PL1','Programación',3,'obligatoria',1),
				  ('A3','PL1','Estructuras',3,'obligatoria',2)""");
		jdbc.update("INSERT INTO periodo_academico VALUES ('2026-2',2026,2,'2026-08-01','2026-12-15','en_curso')");
	}

	/** Crea un estudiante con su método de acceso local, como hace el registro. */
	protected int cuenta(String correo) {
		int id = jdbc.queryForObject("""
				INSERT INTO estudiante (nombres, apellidos, correo_institucional, consentimiento_datos, fecha_consentimiento, estado_cuenta)
				VALUES ('Nombre', 'Apellido', ?, TRUE, now(), 'activa') RETURNING id_estudiante""", Integer.class, correo);
		jdbc.update("""
				INSERT INTO credencial_acceso (id_estudiante, proveedor, hash_contrasena, correo_proveedor, correo_verificado)
				VALUES (?, 'local', ?, ?, TRUE)""", id, HASH_BCRYPT, correo);
		return id;
	}

	protected int matricula(int estudiante, String asignatura) {
		return jdbc.queryForObject("""
				INSERT INTO matricula_asignatura (id_estudiante, codigo_asignatura, codigo_periodo)
				VALUES (?, ?, '2026-2') RETURNING id_matricula""", Integer.class, estudiante, asignatura);
	}

	/** Estructura de tres cortes (30, 30, 40) con una actividad del 100 % en cada uno. */
	protected void estructuraDeEvaluacion(int matricula) {
		for (int categoria = 1; categoria <= 3; categoria++) {
			jdbc.update("INSERT INTO categoria_evaluacion (id_matricula, consec_categoria, nombre_categoria, porcentaje) VALUES (?, ?, ?, ?)",
					matricula, categoria, "Corte " + categoria, categoria == 3 ? 40 : 30);
			jdbc.update("INSERT INTO actividad_evaluativa (id_matricula, consec_categoria, consec_actividad, nombre_actividad, porcentaje) VALUES (?, ?, 1, 'Parcial', 100)",
					matricula, categoria);
		}
	}

	protected void marcarComoCalificada(int matricula, int categoria, int actividad) {
		jdbc.update("UPDATE actividad_evaluativa SET estado_entrega = 'calificada' WHERE id_matricula = ? AND consec_categoria = ? AND consec_actividad = ?",
				matricula, categoria, actividad);
	}

	protected int contar(String sql, Object... argumentos) {
		Integer total = jdbc.queryForObject(sql, Integer.class, argumentos);
		assertThat(total).isNotNull();
		return total;
	}
}
