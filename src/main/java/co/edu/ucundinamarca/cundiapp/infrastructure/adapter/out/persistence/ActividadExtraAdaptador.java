package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.ActividadExtraRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadExtra;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda y lee las actividades extra del estudiante (SCRUM-266).
 *
 * <p>Con JdbcTemplate, como {@link HorarioAdaptador}: es una tabla sin relaciones que valga la pena mapear.
 */
@Component
class ActividadExtraAdaptador implements ActividadExtraRepositorio {

	private final JdbcTemplate jdbc;

	ActividadExtraAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	@Transactional
	public Optional<List<ActividadExtra>> guardarSiCaben(int idEstudiante, List<ActividadExtra> actividades, int maximo) {
		// Bloquear la fila del estudiante pone en fila a las peticiones del mismo estudiante: la segunda cuenta
		// después de que la primera guardó. Sin esto las dos contarían cero y las dos guardarían.
		jdbc.queryForList("SELECT id_estudiante FROM estudiante WHERE id_estudiante = ? FOR UPDATE", idEstudiante);
		Integer existentes = jdbc.queryForObject(
				"SELECT COUNT(*) FROM actividad_extra WHERE id_estudiante = ?", Integer.class, idEstudiante);
		if ((existentes == null ? 0 : existentes) + actividades.size() > maximo) {
			return Optional.empty();
		}
		List<ActividadExtra> guardadas = new ArrayList<>();
		for (ActividadExtra actividad : actividades) {
			Integer id = jdbc.queryForObject("""
					INSERT INTO actividad_extra
					       (id_estudiante, nombre, dia_semana, hora_inicio, hora_fin, lugar, responsable)
					VALUES (?, ?, ?, ?, ?, ?, ?)
					RETURNING id_actividad_extra""",
					Integer.class, idEstudiante, actividad.nombre(), actividad.dia().valorEnBd(),
					java.sql.Time.valueOf(actividad.horaInicio()), java.sql.Time.valueOf(actividad.horaFin()),
					actividad.lugar(), actividad.responsable());
			guardadas.add(new ActividadExtra(id, idEstudiante, actividad.nombre(), actividad.dia(), actividad.horaInicio(),
					actividad.horaFin(), actividad.lugar(), actividad.responsable()));
		}
		return Optional.of(List.copyOf(guardadas));
	}

	@Override
	public List<ActividadExtra> de(int idEstudiante) {
		return jdbc.query("""
				SELECT id_actividad_extra, id_estudiante, nombre, dia_semana, hora_inicio, hora_fin, lugar, responsable
				  FROM actividad_extra
				 WHERE id_estudiante = ?""",
				(fila, numero) -> aActividad(fila), idEstudiante);
	}

	@Override
	public boolean quitar(int idEstudiante, int idActividad) {
		return jdbc.update(
				"DELETE FROM actividad_extra WHERE id_actividad_extra = ? AND id_estudiante = ?",
				idActividad, idEstudiante) > 0;
	}

	private static ActividadExtra aActividad(ResultSet fila) throws SQLException {
		return new ActividadExtra(
				fila.getInt("id_actividad_extra"),
				fila.getInt("id_estudiante"),
				fila.getString("nombre"),
				DiaDeLaSemana.desdeBd(fila.getString("dia_semana")),
				fila.getObject("hora_inicio", LocalTime.class),
				fila.getObject("hora_fin", LocalTime.class),
				fila.getString("lugar"),
				fila.getString("responsable"));
	}
}
