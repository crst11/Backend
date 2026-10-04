package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.NotaConfirmada;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.PeriodoConfirmado;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.Resultado;
import co.edu.ucundinamarca.cundiapp.application.port.in.DeshacerImportacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaDetectada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeImportacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja guardada una importación confirmada y sabe deshacerla (SCRUM-23 y SCRUM-24).
 *
 * <p>Va con JdbcTemplate y no con JPA porque esto es carga de datos: inserciones en lote con
 * "insertar o actualizar" sobre varias tablas. Con entidades quedaría más código para lo mismo.
 *
 * <p>Guardar y deshacer ocurren cada uno en una sola transacción: a medias dejarían el historial
 * diciendo cosas que no son.
 */
@Component
class ImportacionAdaptador implements ImportacionRepositorio {

	private static final RowMapper<Importacion> A_IMPORTACION = (fila, numero) -> new Importacion(
			fila.getInt("id_importacion"),
			fila.getString("tipo_reporte"),
			fila.getString("nombre_archivo"),
			fila.getTimestamp("fecha_carga").toInstant(),
			EstadoDeImportacion.desdeBd(fila.getString("estado_importacion")),
			fila.getInt("registros_detectados"),
			fila.getInt("registros_confirmados"));

	private final JdbcTemplate jdbc;

	ImportacionAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	@Transactional
	public Resultado guardar(
			int idEstudiante, String nombreArchivo, int detectadas, List<PeriodoConfirmado> periodos) {
		int confirmadas = periodos.stream().mapToInt(periodo -> periodo.notas().size()).sum();
		int idImportacion = crearImportacion(idEstudiante, nombreArchivo, detectadas, confirmadas);

		int guardadas = 0;
		int actualizadas = 0;
		for (PeriodoConfirmado periodo : periodos) {
			asegurarPeriodo(periodo.codigo());
			for (NotaConfirmada nota : periodo.notas()) {
				respaldarMatricula(idEstudiante, idImportacion, periodo.codigo(), nota.codigoAsignatura());
				if (insertarOActualizar(idEstudiante, idImportacion, periodo.codigo(), nota)) {
					guardadas++;
				} else {
					actualizadas++;
				}
			}
			respaldarResumen(idEstudiante, idImportacion, periodo.codigo());
			guardarResumen(idEstudiante, idImportacion, periodo);
		}

		return new Resultado(idImportacion, guardadas, actualizadas, detectadas - confirmadas);
	}

	@Override
	public List<Importacion> deEstudiante(int idEstudiante) {
		return jdbc.query("""
				SELECT id_importacion, tipo_reporte, nombre_archivo, fecha_carga, estado_importacion,
				       registros_detectados, registros_confirmados
				  FROM cundiapp.importacion
				 WHERE id_estudiante = ?
				 ORDER BY fecha_carga DESC, id_importacion DESC""",
				A_IMPORTACION, idEstudiante);
	}

	@Override
	public Optional<Importacion> buscar(int idEstudiante, int idImportacion) {
		return jdbc.query("""
				SELECT id_importacion, tipo_reporte, nombre_archivo, fecha_carga, estado_importacion,
				       registros_detectados, registros_confirmados
				  FROM cundiapp.importacion
				 WHERE id_estudiante = ? AND id_importacion = ?""",
				A_IMPORTACION, idEstudiante, idImportacion).stream().findFirst();
	}

	@Override
	@Transactional
	public DeshacerImportacion.Resultado revertir(int idEstudiante, int idImportacion) {
		// Lo que la carga creó se elimina; lo que pisó vuelve a su valor.
		int eliminadas = jdbc.update("""
				DELETE FROM cundiapp.matricula_asignatura m
				 USING cundiapp.respaldo_matricula r
				 WHERE r.id_importacion = ? AND r.existia_antes = false
				   AND m.id_estudiante = r.id_estudiante
				   AND m.codigo_asignatura = r.codigo_asignatura
				   AND m.codigo_periodo = r.codigo_periodo
				   AND m.id_estudiante = ?""", idImportacion, idEstudiante);

		int restauradas = jdbc.update("""
				UPDATE cundiapp.matricula_asignatura m
				   SET nota_definitiva = r.nota_anterior,
				       estado_matricula = r.estado_anterior,
				       fuente_notas = COALESCE(r.fuente_anterior, 'manual'),
				       id_importacion = NULL,
				       actualizado_en = now()
				  FROM cundiapp.respaldo_matricula r
				 WHERE r.id_importacion = ? AND r.existia_antes = true
				   AND m.id_estudiante = r.id_estudiante
				   AND m.codigo_asignatura = r.codigo_asignatura
				   AND m.codigo_periodo = r.codigo_periodo
				   AND m.id_estudiante = ?""", idImportacion, idEstudiante);

		jdbc.update("""
				DELETE FROM cundiapp.resumen_periodo rp
				 USING cundiapp.respaldo_resumen_periodo r
				 WHERE r.id_importacion = ? AND r.existia_antes = false
				   AND rp.id_estudiante = r.id_estudiante AND rp.codigo_periodo = r.codigo_periodo
				   AND rp.id_estudiante = ?""", idImportacion, idEstudiante);

		jdbc.update("""
				UPDATE cundiapp.resumen_periodo rp
				   SET creditos_matriculados = r.creditos_matriculados_anterior,
				       creditos_aprobados = r.creditos_aprobados_anterior,
				       promedio_periodo = r.promedio_periodo_anterior,
				       promedio_acumulado = r.promedio_acumulado_anterior,
				       fuente = COALESCE(r.fuente_anterior, 'manual'),
				       id_importacion = NULL
				  FROM cundiapp.respaldo_resumen_periodo r
				 WHERE r.id_importacion = ? AND r.existia_antes = true
				   AND rp.id_estudiante = r.id_estudiante AND rp.codigo_periodo = r.codigo_periodo
				   AND rp.id_estudiante = ?""", idImportacion, idEstudiante);

		jdbc.update("""
				UPDATE cundiapp.importacion
				   SET estado_importacion = ?,
				       mensaje_resultado = 'Deshecha por el estudiante'
				 WHERE id_importacion = ? AND id_estudiante = ?""",
				EstadoDeImportacion.REVERTIDA.valorEnBd(), idImportacion, idEstudiante);

		return new DeshacerImportacion.Resultado(idImportacion, eliminadas, restauradas);
	}

	private int crearImportacion(int idEstudiante, String nombreArchivo, int detectadas, int confirmadas) {
		return jdbc.queryForObject("""
				INSERT INTO cundiapp.importacion
				       (id_estudiante, tipo_reporte, nombre_archivo, estado_importacion,
				        registros_detectados, registros_confirmados)
				VALUES (?, 'historial', ?, 'confirmada', ?, ?)
				RETURNING id_importacion""",
				Integer.class, idEstudiante, nombreArchivo, detectadas, confirmadas);
	}

	/**
	 * El reporte dice qué períodos cursó el estudiante, pero no sus fechas, y el esquema las exige.
	 * Se derivan de la convención de la universidad (ver {@link PeriodoAcademico}) y el período
	 * queda finalizado si ya pasó. Si el período ya existe no se toca: puede haberlo creado otra
	 * importación, o el calendario académico el día que se cargue de verdad.
	 */
	private void asegurarPeriodo(String codigo) {
		PeriodoAcademico periodo = PeriodoAcademico.desdeCodigo(codigo);
		String estado = periodo.terminoAntesDe(LocalDate.now()) ? "finalizado" : "en_curso";
		jdbc.update("""
				INSERT INTO cundiapp.periodo_academico
				       (codigo_periodo, anio, semestre, fecha_inicio, fecha_fin, estado_periodo)
				VALUES (?, ?, ?, ?, ?, ?)
				ON CONFLICT (codigo_periodo) DO NOTHING""",
				periodo.codigo(), periodo.anio(), periodo.semestre(),
				periodo.inicioAproximado(), periodo.finAproximado(), estado);
	}

	/** Guarda cómo estaba la matrícula antes de tocarla, para poder devolverla (SCRUM-24). */
	private void respaldarMatricula(
			int idEstudiante, int idImportacion, String codigoPeriodo, String codigoAsignatura) {
		jdbc.update("""
				INSERT INTO cundiapp.respaldo_matricula
				       (id_importacion, id_estudiante, codigo_asignatura, codigo_periodo,
				        existia_antes, estado_anterior, nota_anterior, fuente_anterior)
				SELECT ?, ?, ?, ?, m.id_matricula IS NOT NULL, m.estado_matricula, m.nota_definitiva, m.fuente_notas
				  FROM (SELECT 1) AS siempre
				  LEFT JOIN cundiapp.matricula_asignatura m
				         ON m.id_estudiante = ? AND m.codigo_asignatura = ? AND m.codigo_periodo = ?
				ON CONFLICT (id_importacion, id_estudiante, codigo_asignatura, codigo_periodo) DO NOTHING""",
				idImportacion, idEstudiante, codigoAsignatura, codigoPeriodo,
				idEstudiante, codigoAsignatura, codigoPeriodo);
	}

	private void respaldarResumen(int idEstudiante, int idImportacion, String codigoPeriodo) {
		jdbc.update("""
				INSERT INTO cundiapp.respaldo_resumen_periodo
				       (id_importacion, id_estudiante, codigo_periodo, existia_antes,
				        creditos_matriculados_anterior, creditos_aprobados_anterior,
				        promedio_periodo_anterior, promedio_acumulado_anterior, fuente_anterior)
				SELECT ?, ?, ?, rp.codigo_periodo IS NOT NULL,
				       rp.creditos_matriculados, rp.creditos_aprobados,
				       rp.promedio_periodo, rp.promedio_acumulado, rp.fuente
				  FROM (SELECT 1) AS siempre
				  LEFT JOIN cundiapp.resumen_periodo rp
				         ON rp.id_estudiante = ? AND rp.codigo_periodo = ?
				ON CONFLICT (id_importacion, id_estudiante, codigo_periodo) DO NOTHING""",
				idImportacion, idEstudiante, codigoPeriodo, idEstudiante, codigoPeriodo);
	}

	/** Devuelve true si la matrícula es nueva, false si ya existía y se actualizó. */
	private boolean insertarOActualizar(
			int idEstudiante, int idImportacion, String codigoPeriodo, NotaConfirmada nota) {
		String estado = estadoDe(nota.nota());
		int insertadas = jdbc.update("""
				INSERT INTO cundiapp.matricula_asignatura
				       (id_estudiante, codigo_asignatura, codigo_periodo, id_importacion,
				        estado_matricula, nota_definitiva, fuente_notas)
				VALUES (?, ?, ?, ?, ?, ?, 'importacion')
				ON CONFLICT (id_estudiante, codigo_asignatura, codigo_periodo) DO NOTHING""",
				idEstudiante, nota.codigoAsignatura(), codigoPeriodo, idImportacion, estado, nota.nota());
		if (insertadas == 1) {
			return true;
		}
		jdbc.update("""
				UPDATE cundiapp.matricula_asignatura
				   SET nota_definitiva = ?, estado_matricula = ?, id_importacion = ?,
				       fuente_notas = 'importacion', actualizado_en = now()
				 WHERE id_estudiante = ? AND codigo_asignatura = ? AND codigo_periodo = ?""",
				nota.nota(), estado, idImportacion,
				idEstudiante, nota.codigoAsignatura(), codigoPeriodo);
		return false;
	}

	private void guardarResumen(int idEstudiante, int idImportacion, PeriodoConfirmado periodo) {
		jdbc.update("""
				INSERT INTO cundiapp.resumen_periodo
				       (id_estudiante, codigo_periodo, id_importacion, creditos_matriculados,
				        creditos_aprobados, promedio_periodo, promedio_acumulado, fuente)
				VALUES (?, ?, ?, ?, ?, ?, ?, 'importacion')
				ON CONFLICT (id_estudiante, codigo_periodo) DO UPDATE
				   SET id_importacion = EXCLUDED.id_importacion,
				       creditos_matriculados = EXCLUDED.creditos_matriculados,
				       creditos_aprobados = EXCLUDED.creditos_aprobados,
				       promedio_periodo = EXCLUDED.promedio_periodo,
				       promedio_acumulado = EXCLUDED.promedio_acumulado,
				       fuente = 'importacion'""",
				idEstudiante, periodo.codigo(), idImportacion, periodo.creditosMatriculados(),
				periodo.creditosAprobados(), periodo.promedioPeriodo(), periodo.promedioAcumulado());
	}

	private static String estadoDe(BigDecimal nota) {
		if (nota == null) {
			return EstadoDeAsignatura.EN_CURSO.valorEnBd();
		}
		return nota.compareTo(AsignaturaDetectada.NOTA_MINIMA_APROBATORIA) >= 0
				? EstadoDeAsignatura.APROBADA.valorEnBd()
				: EstadoDeAsignatura.REPROBADA.valorEnBd();
	}

}
