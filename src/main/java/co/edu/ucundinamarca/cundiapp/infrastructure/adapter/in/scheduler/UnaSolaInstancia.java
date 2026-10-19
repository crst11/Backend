package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.scheduler;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.stereotype.Component;

/**
 * Hace que una tarea programada corra en una sola instancia a la vez (12 factores, factor 9, SCRUM-220). Con dos
 * instancias del backend, las dos disparan la ronda de avisos y las dos leen los mismos avisos pendientes: el
 * estudiante recibiría el push dos veces y los intentos se contarían dos veces.
 *
 * <p>Usa un candado asesor de PostgreSQL de transacción ({@code pg_try_advisory_xact_lock}), sin tablas ni dependencias
 * nuevas. La transacción vive en una conexión propia mientras dura la tarea; la tarea hace su trabajo por sus
 * conexiones de siempre. El candado se suelta al terminar la transacción y, si la instancia muere a mitad de la
 * ronda, al cerrarse la conexión: nunca queda un candado huérfano. Al ir dentro de una transacción también sirve
 * con el Transaction pooler de Supabase.
 *
 * <p>La instancia que no lo obtiene se salta la ronda; la siguiente vuelta lo vuelve a intentar.
 */
@Component
@ConditionalOnProperty(name = "cundiapp.avisos.programados", havingValue = "true", matchIfMissing = true)
class UnaSolaInstancia {

	private static final Logger log = LoggerFactory.getLogger(UnaSolaInstancia.class);

	/**
	 * Conexión PROPIA, fuera del pool de la aplicación (SCRUM-228). El candado se sostiene durante toda la ronda, que incluye envíos
	 * de Web Push de hasta 10 s cada uno; si saliera del pool de la aplicación (5 conexiones en preproducción y producción), una
	 * ronda lenta dejaría al 20 % del pool ocupado por minutos y las peticiones de los estudiantes fallarían por falta de conexión.
	 * Un pool de una sola conexión, perezoso (no conecta hasta la primera ronda) y que la devuelve al servidor entre rondas.
	 */
	private final HikariDataSource datos;

	UnaSolaInstancia(JdbcConnectionDetails conexion) {
		HikariDataSource pool = new HikariDataSource();
		pool.setJdbcUrl(conexion.getJdbcUrl());
		pool.setUsername(conexion.getUsername());
		pool.setPassword(conexion.getPassword());
		pool.setPoolName("cundiapp-candados");
		pool.setMaximumPoolSize(1);
		pool.setMinimumIdle(0);
		// La ronda corre cada 5 minutos: entre una y otra no hace falta tener una sesión abierta en el servidor.
		pool.setIdleTimeout(10_000);
		pool.setConnectionTimeout(5_000);
		this.datos = pool;
	}

	/** Cierra la conexión propia al apagar la aplicación (y en las pruebas). */
	@PreDestroy
	void cerrar() {
		datos.close();
	}

	/**
	 * Corre {@code tarea} si ninguna otra instancia tiene el candado {@code llave}. Devuelve falso si lo tenía otra
	 * y la tarea no corrió. Si la tarea falla, el candado se suelta y la excepción sale tal cual.
	 */
	boolean ejecutar(long llave, Runnable tarea) {
		try (Connection conexion = datos.getConnection()) {
			conexion.setAutoCommit(false);
			try {
				if (!obtener(conexion, llave)) {
					return false;
				}
				tarea.run();
				return true;
			} finally {
				soltar(conexion);
			}
		} catch (SQLException e) {
			// Sin base de datos no hay ronda que repartir: se anota y la siguiente vuelta lo intenta de nuevo.
			throw new IllegalStateException("No pude tomar el candado de la tarea programada", e);
		}
	}

	private static boolean obtener(Connection conexion, long llave) throws SQLException {
		try (PreparedStatement consulta = conexion.prepareStatement("SELECT pg_try_advisory_xact_lock(?)")) {
			consulta.setLong(1, llave);
			try (ResultSet resultado = consulta.executeQuery()) {
				return resultado.next() && resultado.getBoolean(1);
			}
		}
	}

	/** Cierra la transacción del candado. Si no se puede, la conexión se descarta al cerrarse y el candado cae con ella. */
	private static void soltar(Connection conexion) {
		try {
			conexion.rollback();
		} catch (SQLException e) {
			log.warn("No pude cerrar la transacción del candado; la conexión se descarta", e);
		}
	}
}
