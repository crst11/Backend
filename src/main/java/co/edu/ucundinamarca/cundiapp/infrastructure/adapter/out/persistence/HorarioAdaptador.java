package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.Docente;
import co.edu.ucundinamarca.cundiapp.domain.model.EspacioFisico;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda y lee el horario del estudiante (RF04, SCRUM-25).
 *
 * <p>Con JdbcTemplate, como el resto de lo que mueve árboles de varias tablas: un bloque toca tres
 * (el bloque, el espacio físico y el docente) y con entidades y cascadas sería más código para lo
 * mismo.
 *
 * <p>El horario son las clases de las asignaturas <em>en curso</em>. Una materia ya terminada
 * conserva sus bloques en la base de datos, pero no salen en el horario de hoy.
 */
@Component
class HorarioAdaptador implements HorarioRepositorio {

	private static final String CLASES = """
			SELECT m.id_matricula, m.codigo_asignatura, a.nombre_asignatura,
			       b.consec_bloque, b.dia_semana, b.hora_inicio, b.hora_fin,
			       e.nomenclatura, e.bloque AS bloque_del_edificio, e.sede,
			       d.nombres, d.apellidos
			  FROM bloque_de_horario b
			  JOIN matricula_asignatura m ON m.id_matricula = b.id_matricula
			  JOIN asignatura a ON a.codigo_asignatura = m.codigo_asignatura
			  LEFT JOIN espacio_fisico e ON e.id_espacio = b.id_espacio
			  LEFT JOIN docente d ON d.id_docente = b.id_docente
			 WHERE m.id_estudiante = ? AND m.estado_matricula = 'en_curso'""";

	private final JdbcTemplate jdbc;

	HorarioAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<ClaseDelHorario> clasesDe(int idEstudiante) {
		return jdbc.query(CLASES, (fila, numero) -> aClase(fila), idEstudiante);
	}

	private static ClaseDelHorario aClase(ResultSet fila) throws SQLException {
		String salon = fila.getString("nomenclatura");
		EspacioFisico espacio = salon == null
				? null
				: new EspacioFisico(salon, fila.getString("bloque_del_edificio"), fila.getString("sede"));
		String nombres = fila.getString("nombres");
		Docente docente = nombres == null ? null : new Docente(nombres, fila.getString("apellidos"));
		var bloque = new BloqueDeHorario(
				fila.getInt("consec_bloque"),
				DiaDeLaSemana.desdeBd(fila.getString("dia_semana")),
				fila.getObject("hora_inicio", java.time.LocalTime.class),
				fila.getObject("hora_fin", java.time.LocalTime.class),
				espacio,
				docente);
		return new ClaseDelHorario(
				fila.getInt("id_matricula"), fila.getString("codigo_asignatura"), fila.getString("nombre_asignatura"), bloque);
	}

	@Override
	public Optional<AsignaturaEnCurso> asignaturaEnCurso(int idEstudiante, int idMatricula) {
		return jdbc.query("""
				SELECT m.id_matricula, m.codigo_asignatura, a.nombre_asignatura
				  FROM matricula_asignatura m
				  JOIN asignatura a ON a.codigo_asignatura = m.codigo_asignatura
				 WHERE m.id_matricula = ? AND m.id_estudiante = ? AND m.estado_matricula = 'en_curso'""",
				(fila, numero) -> new AsignaturaEnCurso(
						fila.getInt("id_matricula"), fila.getString("codigo_asignatura"), fila.getString("nombre_asignatura")),
				idMatricula, idEstudiante).stream().findFirst();
	}

	@Override
	public int siguienteConsecutivo(int idMatricula) {
		Integer siguiente = jdbc.queryForObject(
				"SELECT COALESCE(MAX(consec_bloque), 0) + 1 FROM bloque_de_horario WHERE id_matricula = ?",
				Integer.class, idMatricula);
		return siguiente == null ? 1 : siguiente;
	}

	@Override
	@Transactional
	public void guardar(int idEstudiante, int idMatricula, BloqueDeHorario bloque) {
		Integer idEspacio = bloque.espacio().map(this::idDelEspacio).orElse(null);
		Integer idDocente = bloque.docente().map(this::idDelDocente).orElse(null);
		jdbc.update("""
				INSERT INTO bloque_de_horario
				       (id_matricula, consec_bloque, id_espacio, id_docente, dia_semana, hora_inicio, hora_fin)
				VALUES (?, ?, ?, ?, ?, ?, ?)""",
				idMatricula, bloque.consecutivo(), idEspacio, idDocente, bloque.dia().valorEnBd(),
				java.sql.Time.valueOf(bloque.horaInicio()), java.sql.Time.valueOf(bloque.horaFin()));
	}

	/** El salón es el mismo para todos los estudiantes: si ya está se reutiliza, y si no se crea. */
	private int idDelEspacio(EspacioFisico espacio) {
		List<Integer> existentes = jdbc.queryForList("""
				SELECT id_espacio FROM espacio_fisico
				 WHERE nomenclatura = ? AND sede = ? AND bloque IS NOT DISTINCT FROM ?
				 ORDER BY id_espacio LIMIT 1""",
				Integer.class, espacio.salon(), espacio.sede(), espacio.bloque());
		if (!existentes.isEmpty()) {
			return existentes.getFirst();
		}
		return jdbc.queryForObject("""
				INSERT INTO espacio_fisico (nomenclatura, bloque, sede)
				VALUES (?, ?, ?) RETURNING id_espacio""",
				Integer.class, espacio.salon(), espacio.bloque(), espacio.sede());
	}

	/**
	 * Se identifica por el nombre normalizado, que es único: quien llegó primero deja escrito cómo se
	 * ve el nombre. El UPDATE no cambia nada; está para que RETURNING devuelva la fila existente.
	 */
	private int idDelDocente(Docente docente) {
		return jdbc.queryForObject("""
				INSERT INTO docente (nombres, apellidos, nombre_normalizado)
				VALUES (?, ?, ?)
				ON CONFLICT (nombre_normalizado) DO UPDATE SET nombre_normalizado = EXCLUDED.nombre_normalizado
				RETURNING id_docente""",
				Integer.class, docente.nombres(), docente.apellidos(), docente.nombreNormalizado());
	}

	@Override
	public boolean quitar(int idEstudiante, int idMatricula, int consecutivoDelBloque) {
		return jdbc.update("""
				DELETE FROM bloque_de_horario b
				 USING matricula_asignatura m
				 WHERE m.id_matricula = b.id_matricula AND m.id_estudiante = ?
				   AND b.id_matricula = ? AND b.consec_bloque = ?""",
				idEstudiante, idMatricula, consecutivoDelBloque) > 0;
	}
}
