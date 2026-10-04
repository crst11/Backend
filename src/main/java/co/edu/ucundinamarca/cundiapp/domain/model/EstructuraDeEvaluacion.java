package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.List;
import java.util.Optional;

/**
 * Cómo se evalúa una asignatura matriculada: el árbol de dos niveles de RF05 (SCRUM-27).
 *
 * <p>Nunca son "tres cortes fijos". La universidad usa 30/30/40 como punto de partida, pero cada
 * docente arma lo suyo, así que el estudiante cambia los nombres, los pesos y cuántos son.
 *
 * <p>Una estructura vacía es válida y es el estado normal de toda matrícula recién importada: el
 * estudiante no ha dicho todavía cómo lo evalúan. Lo que no vale es dejarla a medias, porque un
 * árbol cuyos pesos no suman 100 % calcularía una nota final que no es la de nadie.
 */
public record EstructuraDeEvaluacion(int idMatricula, List<CategoriaDeEvaluacion> categorias) {

	public EstructuraDeEvaluacion {
		if (idMatricula <= 0) {
			throw new ReglaDeNegocioVioladaException("La estructura de evaluación va sobre una matrícula");
		}
		categorias = categorias == null ? List.of() : List.copyOf(categorias);
		if (!categorias.isEmpty()) {
			String donde = "las categorías de la asignatura";
			ReglasDeLaEstructura.consecutivosSinRepetir(
					categorias.stream().map(CategoriaDeEvaluacion::consecutivo).toList(), donde);
			ReglasDeLaEstructura.sumanExactamente100(
					categorias.stream().map(CategoriaDeEvaluacion::porcentaje).toList(), donde);
		}
	}

	/**
	 * El punto de partida de una asignatura: los cortes de la plantilla, sin actividades.
	 *
	 * <p>Quedan marcados como venidos de la plantilla para que la pantalla pueda avisar que todavía
	 * son los valores por defecto y conviene confirmarlos con el docente.
	 */
	public static EstructuraDeEvaluacion desdePlantilla(int idMatricula, PlantillaDeEvaluacion plantilla) {
		if (plantilla == null) {
			throw new ReglaDeNegocioVioladaException("No hay plantilla de evaluación con la que empezar");
		}
		List<CategoriaDeEvaluacion> categorias = plantilla.items().stream()
				.map(item -> new CategoriaDeEvaluacion(item.consecutivo(), item.nombre(), item.porcentaje(),
						OrigenDeCategoria.PLANTILLA, List.of()))
				.toList();
		return new EstructuraDeEvaluacion(idMatricula, categorias);
	}

	/** Falso mientras el estudiante no haya dicho nada sobre cómo lo evalúan en esta asignatura. */
	public boolean estaDefinida() {
		return !categorias.isEmpty();
	}

	public Optional<CategoriaDeEvaluacion> categoriaDe(int consecutivo) {
		return categorias.stream().filter(categoria -> categoria.consecutivo() == consecutivo).findFirst();
	}

	public int totalDeActividades() {
		return categorias.stream().mapToInt(categoria -> categoria.actividades().size()).sum();
	}
}
