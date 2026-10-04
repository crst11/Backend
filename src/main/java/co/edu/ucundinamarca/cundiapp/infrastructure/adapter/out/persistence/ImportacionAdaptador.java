package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.NotaConfirmada;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.PeriodoConfirmado;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.Resultado;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaDetectada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja guardada una importación confirmada (SCRUM-23).
 *
 * <p>Va con JdbcTemplate y no con JPA porque esto es carga de datos: inserciones en lote con
 * "insertar o actualizar" sobre tres tablas. Con entidades quedaría más código para hacer lo mismo.
 *
 * <p>Todo ocurre en una sola transacción: una importación a medias dejaría el historial diciendo
 * cosas que no son, con unos períodos cargados y otros no.
 */
@Component
class ImportacionAdaptador implements ImportacionRepositorio {

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
				if (insertarOActualizar(idEstudiante, idImportacion, periodo.codigo(), nota)) {
					guardadas++;
				} else {
					actualizadas++;
				}
			}
			guardarResumen(idEstudiante, idImportacion, periodo);
		}

		return new Resultado(idImportacion, guardadas, actualizadas, detectadas - confirmadas);
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
