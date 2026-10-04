package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Un nivel del árbol de evaluación: el corte, el seguimiento, el proyecto (RF05).
 *
 * <p>Las actividades pueden venir vacías, y eso no es un error: el estudiante primero define los
 * cortes y después desglosa qué cae en cada uno. Pero si pone aunque sea una, tienen que sumar
 * 100 % entre ellas, porque si no el corte se calcularía con una parte de su propio peso.
 */
public record CategoriaDeEvaluacion(
		int consecutivo,
		String nombre,
		BigDecimal porcentaje,
		OrigenDeCategoria origen,
		List<ActividadEvaluativa> actividades) {

	/** Lo que cabe en categoria_evaluacion.nombre_categoria. */
	static final int LARGO_DEL_NOMBRE = 80;

	public CategoriaDeEvaluacion {
		ReglasDeLaEstructura.consecutivoValido(consecutivo, "la categoría");
		ReglasDeLaEstructura.nombreValido(nombre, LARGO_DEL_NOMBRE, "la categoría");
		ReglasDeLaEstructura.porcentajeValido(porcentaje, "la categoría «%s»".formatted(nombre));
		if (origen == null) {
			throw new ReglaDeNegocioVioladaException(
					"La categoría «%s» necesita saber si la puso el estudiante o la plantilla".formatted(nombre));
		}
		actividades = actividades == null ? List.of() : List.copyOf(actividades);
		if (!actividades.isEmpty()) {
			String donde = "las actividades de «%s»".formatted(nombre);
			ReglasDeLaEstructura.consecutivosSinRepetir(
					actividades.stream().map(ActividadEvaluativa::consecutivo).toList(), donde);
			ReglasDeLaEstructura.sumanExactamente100(
					actividades.stream().map(ActividadEvaluativa::porcentaje).toList(), donde);
		}
	}

	/** Mientras no tenga actividades, la categoría existe pero todavía no dice cómo se califica. */
	public boolean estaDesglosada() {
		return !actividades.isEmpty();
	}
}
