package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario.Resultado;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja guardado el horario importado del Reporte de Horario (RF04, SCRUM-81).
 *
 * <p>Con JdbcTemplate, como el resto de la carga de datos: toca la matrícula, los bloques, los
 * salones y los docentes en una sola transacción, así que a medias no queda nada.
 *
 * <p>No deja una fila en `importacion` ni respaldo para deshacer: el horario se corrige quitando
 * las clases desde *Mi horario*, y deshacer una carga de clases no necesita la maquinaria de las
 * notas, que sí protege un historial difícil de reconstruir.
 */
@Component
class ImportacionDeHorarioAdaptador implements ImportacionDeHorarioRepositorio {

	private final JdbcTemplate jdbc;
	private final CatalogoDeHorario catalogo;

	ImportacionDeHorarioAdaptador(JdbcTemplate jdbc, CatalogoDeHorario catalogo) {
		this.jdbc = jdbc;
		this.catalogo = catalogo;
	}

	@Override
	public List<MatriculaConClases> matriculasDe(int idEstudiante, PeriodoAcademico periodo) {
		return jdbc.query("""
				SELECT m.codigo_asignatura, m.estado_matricula,
				       (SELECT COUNT(*) FROM bloque_de_horario b WHERE b.id_matricula = m.id_matricula) AS clases
				  FROM matricula_asignatura m
				 WHERE m.id_estudiante = ? AND m.codigo_periodo = ?""",
				(fila, numero) -> new MatriculaConClases(
						fila.getString("codigo_asignatura"), fila.getString("estado_matricula"), fila.getInt("clases")),
				idEstudiante, periodo.codigo());
	}

	@Override
	@Transactional
	public Resultado guardar(int idEstudiante, PeriodoAcademico periodo, List<AsignaturaConHorario> asignaturas) {
		catalogo.asegurarPeriodo(periodo);

		int nuevas = 0;
		int actualizadas = 0;
		int omitidas = 0;
		int clases = 0;
		for (AsignaturaConHorario asignatura : asignaturas) {
			Optional<Matricula> existente = buscar(idEstudiante, periodo, asignatura.codigo());
			int idMatricula;
			if (existente.isEmpty()) {
				idMatricula = crear(idEstudiante, periodo, asignatura);
				nuevas++;
			} else if (existente.get().enCurso()) {
				idMatricula = existente.get().id();
				reemplazarClases(idMatricula, asignatura);
				actualizadas++;
			} else {
				// Ya cerró con nota: sus clases no son parte del horario de hoy y no se tocan.
				omitidas++;
				continue;
			}
			for (BloqueDeHorario bloque : asignatura.bloques()) {
				guardarBloque(idMatricula, bloque);
				clases++;
			}
		}
		return new Resultado(nuevas, actualizadas, omitidas, clases);
	}

	private record Matricula(int id, String estado) {

		boolean enCurso() {
			return "en_curso".equals(estado);
		}
	}

	private Optional<Matricula> buscar(int idEstudiante, PeriodoAcademico periodo, String codigoAsignatura) {
		return jdbc.query("""
				SELECT id_matricula, estado_matricula FROM matricula_asignatura
				 WHERE id_estudiante = ? AND codigo_asignatura = ? AND codigo_periodo = ?""",
				(fila, numero) -> new Matricula(fila.getInt("id_matricula"), fila.getString("estado_matricula")),
				idEstudiante, codigoAsignatura, periodo.codigo()).stream().findFirst();
	}

	private int crear(int idEstudiante, PeriodoAcademico periodo, AsignaturaConHorario asignatura) {
		return jdbc.queryForObject("""
				INSERT INTO matricula_asignatura
				       (id_estudiante, codigo_asignatura, codigo_periodo, codigo_grupo, estado_matricula)
				VALUES (?, ?, ?, ?, 'en_curso')
				RETURNING id_matricula""",
				Integer.class, idEstudiante, asignatura.codigo(), periodo.codigo(), asignatura.grupo());
	}

	/** El reporte manda: sus clases reemplazan a las que la asignatura tenía, no se suman a ellas. */
	private void reemplazarClases(int idMatricula, AsignaturaConHorario asignatura) {
		jdbc.update("DELETE FROM bloque_de_horario WHERE id_matricula = ?", idMatricula);
		if (asignatura.grupo() != null) {
			jdbc.update(
					"UPDATE matricula_asignatura SET codigo_grupo = ?, actualizado_en = now() WHERE id_matricula = ?",
					asignatura.grupo(), idMatricula);
		}
	}

	private void guardarBloque(int idMatricula, BloqueDeHorario bloque) {
		Integer idEspacio = bloque.espacio().map(catalogo::idDelEspacio).orElse(null);
		Integer idDocente = bloque.docente().map(catalogo::idDelDocente).orElse(null);
		jdbc.update("""
				INSERT INTO bloque_de_horario
				       (id_matricula, consec_bloque, id_espacio, id_docente, dia_semana, hora_inicio, hora_fin)
				VALUES (?, ?, ?, ?, ?, ?, ?)""",
				idMatricula, bloque.consecutivo(), idEspacio, idDocente, bloque.dia().valorEnBd(),
				java.sql.Time.valueOf(bloque.horaInicio()), java.sql.Time.valueOf(bloque.horaFin()));
	}
}
