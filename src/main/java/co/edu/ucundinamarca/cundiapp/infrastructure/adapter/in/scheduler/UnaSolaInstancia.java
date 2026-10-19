package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class UnaSolaInstancia {

	private static final Logger log = LoggerFactory.getLogger(UnaSolaInstancia.class);

	private final DataSource datos;

	UnaSolaInstancia(DataSource datos) {
		this.datos = datos;
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
