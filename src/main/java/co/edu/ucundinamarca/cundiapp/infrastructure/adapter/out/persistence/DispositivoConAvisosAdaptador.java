package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.DispositivoConAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Guarda las suscripciones Web Push en {@code dispositivo} (RF10, SCRUM-37). Cada fila es un navegador
 * donde la persona activó los avisos; la dirección ({@code endpoint_push}) lo identifica.
 *
 * <p>Una fila cuya suscripción ya no pasa las reglas del dominio no se entrega: se omite y se anota, en
 * vez de tumbar la entrega de los avisos de todos los demás.
 */
@Component
class DispositivoConAvisosAdaptador implements DispositivoConAvisosRepositorio {

	private static final Logger log = LoggerFactory.getLogger(DispositivoConAvisosAdaptador.class);

	private final JdbcTemplate jdbc;

	DispositivoConAvisosAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public DispositivoConAvisos guardarSuscripcion(
			int idEstudiante, String nombre, SuscripcionPush suscripcion, Instant ahora) {
		OffsetDateTime momento = OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC);
		Integer consecutivo = jdbc.query("""
				UPDATE dispositivo
				   SET nombre_dispositivo = ?, clave_p256dh = ?, clave_auth = ?, fecha_sincronizacion = ?, activo = TRUE
				 WHERE id_estudiante = ? AND endpoint_push = ?
				RETURNING consec_dispositivo""",
				(fila, numero) -> fila.getInt(1),
				nombre, suscripcion.clavePublica(), suscripcion.secretoDeAutenticacion(), momento, idEstudiante,
				suscripcion.endpoint()).stream().findFirst().orElse(null);
		if (consecutivo == null) {
			consecutivo = jdbc.queryForObject("""
					INSERT INTO dispositivo (id_estudiante, consec_dispositivo, nombre_dispositivo, endpoint_push,
					                         clave_p256dh, clave_auth, fecha_sincronizacion, activo)
					SELECT ?, COALESCE(MAX(consec_dispositivo), 0) + 1, ?, ?, ?, ?, ?, TRUE
					  FROM dispositivo WHERE id_estudiante = ?
					RETURNING consec_dispositivo""",
					Integer.class,
					idEstudiante, nombre, suscripcion.endpoint(), suscripcion.clavePublica(),
					suscripcion.secretoDeAutenticacion(), momento, idEstudiante);
		}
		return new DispositivoConAvisos(Objects.requireNonNull(consecutivo), nombre, suscripcion, true, ahora);
	}

	@Override
	public List<DispositivoConAvisos> activos(int idEstudiante) {
		return jdbc.query("""
				SELECT consec_dispositivo, nombre_dispositivo, endpoint_push, clave_p256dh, clave_auth,
				       fecha_sincronizacion, activo
				  FROM dispositivo
				 WHERE id_estudiante = ? AND activo AND endpoint_push IS NOT NULL
				 ORDER BY consec_dispositivo""",
				(fila, numero) -> dispositivo(fila, idEstudiante), idEstudiante)
				.stream().filter(Objects::nonNull).toList();
	}

	@Override
	public void desactivar(int idEstudiante, int consecutivo) {
		jdbc.update("""
				UPDATE dispositivo
				   SET activo = FALSE, endpoint_push = NULL, clave_p256dh = NULL, clave_auth = NULL
				 WHERE id_estudiante = ? AND consec_dispositivo = ?""",
				idEstudiante, consecutivo);
	}

	@Override
	public boolean quitar(int idEstudiante, int consecutivo) {
		return jdbc.update("""
				DELETE FROM dispositivo
				 WHERE id_estudiante = ? AND consec_dispositivo = ? AND activo AND endpoint_push IS NOT NULL""",
				idEstudiante, consecutivo) > 0;
	}

	private static DispositivoConAvisos dispositivo(ResultSet fila, int idEstudiante) throws SQLException {
		int consecutivo = fila.getInt("consec_dispositivo");
		try {
			SuscripcionPush suscripcion = new SuscripcionPush(
					fila.getString("endpoint_push"), fila.getString("clave_p256dh"), fila.getString("clave_auth"));
			OffsetDateTime sincronizacion = fila.getObject("fecha_sincronizacion", OffsetDateTime.class);
			return new DispositivoConAvisos(consecutivo, fila.getString("nombre_dispositivo"), suscripcion,
					fila.getBoolean("activo"), sincronizacion == null ? null : sincronizacion.toInstant());
		} catch (ReglaDeNegocioVioladaException e) {
			log.warn("Se omite el dispositivo {} del estudiante {}: su suscripción ya no es válida", consecutivo, idEstudiante);
			return null;
		}
	}
}
