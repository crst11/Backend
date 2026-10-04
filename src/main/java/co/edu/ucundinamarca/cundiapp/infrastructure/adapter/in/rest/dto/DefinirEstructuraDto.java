package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEntrega;
import co.edu.ucundinamarca.cundiapp.domain.model.OrigenDeCategoria;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeActividad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * El árbol de evaluación que manda la pantalla al guardar (SCRUM-27).
 *
 * <p>Llega completo y reemplaza lo que había. Los números de orden los conserva la pantalla tal
 * como los recibió: una actividad que mantiene el suyo mantiene también la nota que tuviera
 * registrada.
 *
 * <p>Una lista vacía es válida y significa "quiero volver a dejar esta asignatura sin configurar".
 */
public record DefinirEstructuraDto(@NotNull @Valid List<CategoriaSolicitada> categorias) {

	public record CategoriaSolicitada(
			@Positive int consecutivo,
			@NotBlank String nombre,
			@NotNull BigDecimal porcentaje,
			String origen,
			@Valid List<ActividadSolicitada> actividades) {
	}

	public record ActividadSolicitada(
			@Positive int consecutivo,
			@NotBlank String nombre,
			@NotNull BigDecimal porcentaje,
			LocalDate fechaProgramada,
			String tipo,
			String estadoEntrega) {
	}

	public List<CategoriaDeEvaluacion> aCategorias() {
		return categorias.stream().map(DefinirEstructuraDto::aCategoria).toList();
	}

	private static CategoriaDeEvaluacion aCategoria(CategoriaSolicitada categoria) {
		List<ActividadSolicitada> actividades =
				categoria.actividades() == null ? List.of() : categoria.actividades();
		return new CategoriaDeEvaluacion(
				categoria.consecutivo(),
				categoria.nombre(),
				categoria.porcentaje(),
				valorDe(categoria.origen(), OrigenDeCategoria::desdeBd, OrigenDeCategoria.ESTUDIANTE, "origen"),
				actividades.stream().map(DefinirEstructuraDto::aActividad).toList());
	}

	private static ActividadEvaluativa aActividad(ActividadSolicitada actividad) {
		return new ActividadEvaluativa(
				actividad.consecutivo(),
				actividad.nombre(),
				actividad.porcentaje(),
				actividad.fechaProgramada(),
				valorDe(actividad.tipo(), TipoDeActividad::desdeBd, TipoDeActividad.TALLER, "tipo de actividad"),
				valorDe(actividad.estadoEntrega(), EstadoDeEntrega::desdeBd, EstadoDeEntrega.NO_ENTREGADA,
						"estado de entrega"));
	}

	/**
	 * Un valor que no existe en el catálogo es un dato que el estudiante puede corregir, no una
	 * falla del servidor: se traduce a un rechazo que dice cuál fue.
	 */
	private static <T> T valorDe(String valor, Function<String, T> desdeBd, T porDefecto, String que) {
		if (valor == null || valor.isBlank()) {
			return porDefecto;
		}
		try {
			return desdeBd.apply(valor);
		} catch (IllegalArgumentException noExiste) {
			throw new ReglaDeNegocioVioladaException("«%s» no es un %s válido".formatted(valor, que));
		}
	}
}
