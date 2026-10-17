package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.AvisoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEnvio;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAviso;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Guarda y lee los avisos en {@code notificacion} (RF10, SCRUM-37). Con JdbcTemplate: son consultas
 * directas sobre una sola tabla, y la de pendientes por avisar cruza tres.
 *
 * <p>Solo se leen los tipos que la aplicación sabe generar; la base de datos admite también
 * {@code novedad_registrada}, que ninguna historia pide todavía.
 */
@Component
class AvisoAdaptador implements AvisoRepositorio {

	private static final String COLUMNAS =
			"id_notificacion, id_estudiante, tipo_notificacion, titulo, mensaje, fecha_generacion, estado_envio, leida, intentos";
	private static final String TIPOS_CONOCIDOS = "tipo_notificacion IN ('vencimiento_proximo', 'cambio_riesgo')";
	/** La base de datos admite hasta cinco intentos ({@code ck_notificacion_intentos}). */
	private static final int INTENTOS_QUE_ADMITE_LA_BD = 5;

	private final JdbcTemplate jdbc;

	AvisoAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<PendientePorAvisar> pendientesPorAvisar(Instant ahora) {
		OffsetDateTime momento = OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC);
		return jdbc.query("""
				SELECT p.id_estudiante, p.id_pendiente, p.titulo, p.fecha_limite
				  FROM pendiente p
				  JOIN configuracion_estudiante c ON c.id_estudiante = p.id_estudiante
				  JOIN estudiante e ON e.id_estudiante = p.id_estudiante
				 WHERE p.estado_pendiente = 'abierto'
				   AND p.fecha_limite > CAST(? AS timestamptz)
				   AND p.fecha_limite <= CAST(? AS timestamptz) + c.anticipacion_aviso_horas * INTERVAL '1 hour'
				   AND c.notificaciones_activas
				   AND e.estado_cuenta = 'activa'
				   AND NOT EXISTS (SELECT 1 FROM notificacion n
				                    WHERE n.id_pendiente = p.id_pendiente AND n.tipo_notificacion = 'vencimiento_proximo')
				 ORDER BY p.fecha_limite
				 LIMIT 500""",
				(fila, numero) -> new PendientePorAvisar(
						fila.getInt("id_estudiante"),
						fila.getInt("id_pendiente"),
						fila.getString("titulo"),
						fila.getObject("fecha_limite", OffsetDateTime.class).toInstant()),
				momento, momento);
	}

	@Override
	public boolean crearDeVencimiento(Aviso aviso, int idPendiente) {
		OffsetDateTime generado = OffsetDateTime.ofInstant(aviso.fechaGeneracion(), ZoneOffset.UTC);
		return jdbc.update("""
				INSERT INTO notificacion (id_estudiante, id_pendiente, tipo_notificacion, titulo, mensaje,
				                          fecha_programada, fecha_generacion, estado_envio, leida, intentos)
				VALUES (?, ?, 'vencimiento_proximo', ?, ?, ?, ?, 'pendiente', FALSE, 0)
				ON CONFLICT DO NOTHING""",
				aviso.idEstudiante(), idPendiente, aviso.titulo(), aviso.mensaje(), generado, generado) == 1;
	}

	@Override
	public List<Aviso> porEntregar(int limite) {
		return jdbc.query("SELECT " + COLUMNAS + " FROM notificacion WHERE estado_envio = 'pendiente' AND "
				+ TIPOS_CONOCIDOS + " ORDER BY fecha_programada, id_notificacion LIMIT ?", AvisoAdaptador::aviso, limite);
	}

	@Override
	public void marcarEnviado(int idAviso, Instant ahora) {
		jdbc.update("""
				UPDATE notificacion
				   SET estado_envio = 'enviada', fecha_envio = ?, intentos = LEAST(intentos + 1, ?)
				 WHERE id_notificacion = ?""",
				OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC), INTENTOS_QUE_ADMITE_LA_BD, idAviso);
	}

	@Override
	public void registrarFallo(int idAviso, boolean definitivo) {
		jdbc.update("""
				UPDATE notificacion
				   SET intentos = LEAST(intentos + 1, ?),
				       estado_envio = CASE WHEN ? OR intentos + 1 >= ? THEN 'fallida' ELSE 'pendiente' END
				 WHERE id_notificacion = ?""",
				INTENTOS_QUE_ADMITE_LA_BD, definitivo, Aviso.MAXIMO_DE_INTENTOS, idAviso);
	}

	@Override
	public List<Aviso> deEstudiante(int idEstudiante, boolean soloSinLeer, int limite) {
		return jdbc.query("SELECT " + COLUMNAS + " FROM notificacion WHERE id_estudiante = ? AND " + TIPOS_CONOCIDOS
				+ " AND (NOT ? OR NOT leida) ORDER BY fecha_generacion DESC, id_notificacion DESC LIMIT ?",
				AvisoAdaptador::aviso, idEstudiante, soloSinLeer, limite);
	}

	@Override
	public int sinLeer(int idEstudiante) {
		Integer cuantos = jdbc.queryForObject(
				"SELECT COUNT(*) FROM notificacion WHERE id_estudiante = ? AND NOT leida AND " + TIPOS_CONOCIDOS,
				Integer.class, idEstudiante);
		return cuantos == null ? 0 : cuantos;
	}

	@Override
	public boolean marcarLeido(int idEstudiante, int idAviso, boolean leido) {
		return jdbc.update("UPDATE notificacion SET leida = ? WHERE id_notificacion = ? AND id_estudiante = ?",
				leido, idAviso, idEstudiante) > 0;
	}

	@Override
	public void marcarTodosLeidos(int idEstudiante) {
		jdbc.update("UPDATE notificacion SET leida = TRUE WHERE id_estudiante = ? AND NOT leida", idEstudiante);
	}

	private static Aviso aviso(ResultSet fila, int numero) throws SQLException {
		return new Aviso(
				fila.getInt("id_notificacion"),
				fila.getInt("id_estudiante"),
				TipoDeAviso.desdeBd(fila.getString("tipo_notificacion")),
				fila.getString("titulo"),
				fila.getString("mensaje"),
				fila.getObject("fecha_generacion", OffsetDateTime.class).toInstant(),
				EstadoDeEnvio.desdeBd(fila.getString("estado_envio")),
				fila.getBoolean("leida"),
				fila.getInt("intentos"));
	}
}
