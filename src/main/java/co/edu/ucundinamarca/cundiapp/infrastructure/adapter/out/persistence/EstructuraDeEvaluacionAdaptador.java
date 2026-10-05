package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEntrega;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.ItemDePlantilla;
import co.edu.ucundinamarca.cundiapp.domain.model.OrigenDeCategoria;
import co.edu.ucundinamarca.cundiapp.domain.model.PlantillaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeActividad;
import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda y lee el árbol de evaluación de una matrícula (RF05, SCRUM-27).
 *
 * <p>Con JdbcTemplate y no con JPA porque se escribe un árbol completo de una vez sobre dos tablas
 * con clave compuesta; con entidades y cascadas sería más código para lo mismo.
 *
 * <p>Guardar no borra y vuelve a crear: respeta el número de orden de lo que sigue existiendo. Una
 * actividad borrada se lleva en cascada su calificación, así que recrearlas todas le haría perder
 * al estudiante notas que nunca pidió borrar. Las dos comprobaciones del 100 % en la base son
 * triggers DEFERRABLE, de modo que dentro de la transacción la suma puede quedar en tránsito.
 */
@Component
class EstructuraDeEvaluacionAdaptador implements EstructuraDeEvaluacionRepositorio {

	private static final String MATRICULAS = """
			SELECT m.id_matricula, m.codigo_asignatura, a.nombre_asignatura, a.creditos, m.codigo_periodo,
			       m.estado_matricula,
			       (SELECT count(*) FROM categoria_evaluacion c WHERE c.id_matricula = m.id_matricula)
			           AS categorias,
			       (SELECT count(*) FROM actividad_evaluativa v WHERE v.id_matricula = m.id_matricula)
			           AS actividades
			  FROM matricula_asignatura m
			  JOIN asignatura a ON a.codigo_asignatura = m.codigo_asignatura
			 WHERE m.id_estudiante = ?""";

	private static final RowMapper<AsignaturaMatriculada> A_MATRICULA = (fila, numero) -> new AsignaturaMatriculada(
			fila.getInt("id_matricula"),
			fila.getString("codigo_asignatura"),
			fila.getString("nombre_asignatura"),
			fila.getInt("creditos"),
			fila.getString("codigo_periodo"),
			EstadoDeAsignatura.desdeBd(fila.getString("estado_matricula")),
			fila.getInt("categorias"),
			fila.getInt("actividades"));

	private final JdbcTemplate jdbc;

	EstructuraDeEvaluacionAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<AsignaturaMatriculada> matriculasDe(int idEstudiante) {
		return jdbc.query(MATRICULAS + " ORDER BY m.codigo_periodo DESC, a.nombre_asignatura",
				A_MATRICULA, idEstudiante);
	}

	@Override
	public Optional<AsignaturaMatriculada> matricula(int idEstudiante, int idMatricula) {
		return jdbc.query(MATRICULAS + " AND m.id_matricula = ?", A_MATRICULA, idEstudiante, idMatricula)
				.stream()
				.findFirst();
	}

	@Override
	public Optional<EstructuraDeEvaluacion> buscar(int idEstudiante, int idMatricula) {
		List<CategoriaDeEvaluacion> categorias = jdbc.query("""
				SELECT c.consec_categoria, c.nombre_categoria, c.porcentaje, c.origen_categoria
				  FROM categoria_evaluacion c
				  JOIN matricula_asignatura m ON m.id_matricula = c.id_matricula
				 WHERE c.id_matricula = ? AND m.id_estudiante = ?
				 ORDER BY c.consec_categoria""",
				(fila, numero) -> new CategoriaDeEvaluacion(
						fila.getInt("consec_categoria"),
						fila.getString("nombre_categoria"),
						fila.getBigDecimal("porcentaje"),
						OrigenDeCategoria.desdeBd(fila.getString("origen_categoria")),
						actividadesDe(idMatricula, fila.getInt("consec_categoria"))),
				idMatricula, idEstudiante);

		return categorias.isEmpty() ? Optional.empty()
				: Optional.of(new EstructuraDeEvaluacion(idMatricula, categorias));
	}

	private List<ActividadEvaluativa> actividadesDe(int idMatricula, int consecCategoria) {
		return jdbc.query("""
				SELECT consec_actividad, nombre_actividad, porcentaje, fecha_programada, tipo_actividad,
				       estado_entrega
				  FROM actividad_evaluativa
				 WHERE id_matricula = ? AND consec_categoria = ?
				 ORDER BY consec_actividad""",
				(fila, numero) -> new ActividadEvaluativa(
						fila.getInt("consec_actividad"),
						fila.getString("nombre_actividad"),
						fila.getBigDecimal("porcentaje"),
						Optional.ofNullable(fila.getDate("fecha_programada")).map(Date::toLocalDate).orElse(null),
						TipoDeActividad.desdeBd(fila.getString("tipo_actividad")),
						EstadoDeEntrega.desdeBd(fila.getString("estado_entrega"))),
				idMatricula, consecCategoria);
	}

	@Override
	@Transactional
	public void guardar(int idEstudiante, EstructuraDeEvaluacion estructura) {
		int idMatricula = estructura.idMatricula();
		List<Integer> consecutivos = estructura.categorias().stream()
				.map(CategoriaDeEvaluacion::consecutivo)
				.toList();

		borrarCategoriasQueYaNoEstan(idMatricula, consecutivos);

		for (CategoriaDeEvaluacion categoria : estructura.categorias()) {
			guardarCategoria(idMatricula, categoria);
			guardarActividades(idMatricula, categoria);
		}
	}

	private void guardarCategoria(int idMatricula, CategoriaDeEvaluacion categoria) {
		jdbc.update("""
				INSERT INTO categoria_evaluacion
				       (id_matricula, consec_categoria, nombre_categoria, porcentaje, origen_categoria)
				VALUES (?, ?, ?, ?, ?)
				ON CONFLICT (id_matricula, consec_categoria) DO UPDATE
				   SET nombre_categoria = EXCLUDED.nombre_categoria,
				       porcentaje       = EXCLUDED.porcentaje,
				       origen_categoria = EXCLUDED.origen_categoria,
				       actualizado_en   = now()""",
				idMatricula, categoria.consecutivo(), categoria.nombre(), categoria.porcentaje(),
				categoria.origen().valorEnBd());
	}

	/**
	 * El estado de entrega no se toca al reconfigurar: lo mueve el registro de notas, no la
	 * estructura. Pisarlo aquí dejaría una actividad ya calificada marcada como no entregada.
	 */
	private void guardarActividades(int idMatricula, CategoriaDeEvaluacion categoria) {
		List<Integer> consecutivos = categoria.actividades().stream()
				.map(ActividadEvaluativa::consecutivo)
				.toList();
		borrarActividadesQueYaNoEstan(idMatricula, categoria.consecutivo(), consecutivos);

		for (ActividadEvaluativa actividad : categoria.actividades()) {
			jdbc.update("""
					INSERT INTO actividad_evaluativa
					       (id_matricula, consec_categoria, consec_actividad, nombre_actividad, porcentaje,
					        fecha_programada, tipo_actividad, estado_entrega)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					ON CONFLICT (id_matricula, consec_categoria, consec_actividad) DO UPDATE
					   SET nombre_actividad = EXCLUDED.nombre_actividad,
					       porcentaje       = EXCLUDED.porcentaje,
					       fecha_programada = EXCLUDED.fecha_programada,
					       tipo_actividad   = EXCLUDED.tipo_actividad,
					       actualizado_en   = now()""",
					idMatricula, categoria.consecutivo(), actividad.consecutivo(), actividad.nombre(),
					actividad.porcentaje(), actividad.fecha().map(Date::valueOf).orElse(null),
					actividad.tipo().valorEnBd(), actividad.estado().valorEnBd());
		}
	}

	private void borrarCategoriasQueYaNoEstan(int idMatricula, List<Integer> conservar) {
		if (conservar.isEmpty()) {
			jdbc.update("DELETE FROM categoria_evaluacion WHERE id_matricula = ?", idMatricula);
			return;
		}
		jdbc.update("DELETE FROM categoria_evaluacion WHERE id_matricula = ? AND consec_categoria NOT IN ("
				+ huecos(conservar.size()) + ")", parametros(idMatricula, conservar));
	}

	private void borrarActividadesQueYaNoEstan(int idMatricula, int consecCategoria, List<Integer> conservar) {
		if (conservar.isEmpty()) {
			jdbc.update("DELETE FROM actividad_evaluativa WHERE id_matricula = ? AND consec_categoria = ?",
					idMatricula, consecCategoria);
			return;
		}
		jdbc.update("DELETE FROM actividad_evaluativa WHERE id_matricula = ? AND consec_categoria = ?"
				+ " AND consec_actividad NOT IN (" + huecos(conservar.size()) + ")",
				parametros(idMatricula, consecCategoria, conservar));
	}

	/**
	 * Los números de orden se pasan como parámetros, uno por hueco, en vez de armar un arreglo de
	 * PostgreSQL: el controlador recibe los que manda la pantalla y nada de eso entra al SQL.
	 */
	private static String huecos(int cuantos) {
		return String.join(", ", Collections.nCopies(cuantos, "?"));
	}

	private static Object[] parametros(int primero, List<Integer> resto) {
		return Stream.concat(Stream.of(primero), resto.stream()).toArray();
	}

	private static Object[] parametros(int primero, int segundo, List<Integer> resto) {
		return Stream.concat(Stream.of(primero, segundo), resto.stream()).toArray();
	}

	@Override
	public Optional<PlantillaDeEvaluacion> plantillaPredeterminada() {
		return jdbc.query("""
				SELECT id_plantilla, nombre_plantilla, descripcion
				  FROM plantilla_evaluacion
				 WHERE es_predeterminada
				 ORDER BY id_plantilla
				 LIMIT 1""",
				(fila, numero) -> new PlantillaDeEvaluacion(
						fila.getInt("id_plantilla"),
						fila.getString("nombre_plantilla"),
						fila.getString("descripcion"),
						true,
						itemsDe(fila.getInt("id_plantilla"))))
				.stream()
				.findFirst();
	}

	private List<ItemDePlantilla> itemsDe(int idPlantilla) {
		return jdbc.query("""
				SELECT consec_item, nombre_item, porcentaje
				  FROM item_de_plantilla
				 WHERE id_plantilla = ?
				 ORDER BY consec_item""",
				(fila, numero) -> new ItemDePlantilla(
						fila.getInt("consec_item"), fila.getString("nombre_item"), fila.getBigDecimal("porcentaje")),
				idPlantilla);
	}
}
