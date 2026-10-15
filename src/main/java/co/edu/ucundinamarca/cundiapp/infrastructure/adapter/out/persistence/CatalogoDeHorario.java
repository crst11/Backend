package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.domain.model.Docente;
import co.edu.ucundinamarca.cundiapp.domain.model.EspacioFisico;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lo que comparten quienes escriben horarios: el período, el salón y el docente son los mismos
 * para todos los estudiantes, así que se reutilizan si ya existen y solo se crean si faltan
 * (SCRUM-25 y SCRUM-81). Va en una clase aparte para que el horario escrito a mano y el importado
 * del reporte no tengan dos versiones de la misma regla.
 */
@Component
class CatalogoDeHorario {

	private final JdbcTemplate jdbc;

	CatalogoDeHorario(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Los reportes dicen en qué período estamos, pero no sus fechas, y el esquema las exige. Se derivan
	 * de la convención de la universidad (ver {@link PeriodoAcademico}) y el período queda finalizado si
	 * ya pasó. Si el período ya existe no se toca: puede haberlo creado otra importación, o el
	 * calendario académico el día que se cargue de verdad.
	 */
	void asegurarPeriodo(PeriodoAcademico periodo) {
		String estado = periodo.terminoAntesDe(LocalDate.now()) ? "finalizado" : "en_curso";
		jdbc.update("""
				INSERT INTO cundiapp.periodo_academico
				       (codigo_periodo, anio, semestre, fecha_inicio, fecha_fin, estado_periodo)
				VALUES (?, ?, ?, ?, ?, ?)
				ON CONFLICT (codigo_periodo) DO NOTHING""",
				periodo.codigo(), periodo.anio(), periodo.semestre(),
				periodo.inicioAproximado(), periodo.finAproximado(), estado);
	}

	/** El salón es el mismo para todos los estudiantes: si ya está se reutiliza, y si no se crea. */
	int idDelEspacio(EspacioFisico espacio) {
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
	int idDelDocente(Docente docente) {
		return jdbc.queryForObject("""
				INSERT INTO docente (nombres, apellidos, nombre_normalizado)
				VALUES (?, ?, ?)
				ON CONFLICT (nombre_normalizado) DO UPDATE SET nombre_normalizado = EXCLUDED.nombre_normalizado
				RETURNING id_docente""",
				Integer.class, docente.nombres(), docente.apellidos(), docente.nombreNormalizado());
	}
}
