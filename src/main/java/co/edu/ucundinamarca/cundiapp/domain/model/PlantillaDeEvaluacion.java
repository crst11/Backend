package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.List;

/**
 * Un modelo de evaluación que sirve de punto de partida, como el 30/30/40 de la universidad (RF05).
 *
 * <p>La plantilla no evalúa nada por sí sola: se copia en la matrícula y desde ahí el estudiante la
 * ajusta. Por eso sus ítems no tienen actividades, solo los pesos de cada nivel.
 */
public record PlantillaDeEvaluacion(
		int id,
		String nombre,
		String descripcion,
		boolean esPredeterminada,
		List<ItemDePlantilla> items) {

	public PlantillaDeEvaluacion {
		ReglasDeLaEstructura.nombreValido(nombre, 80, "la plantilla");
		items = items == null ? List.of() : List.copyOf(items);
		if (items.isEmpty()) {
			throw new ReglaDeNegocioVioladaException(
					"La plantilla «%s» no sirve de nada sin ítems".formatted(nombre));
		}
		String donde = "los ítems de «%s»".formatted(nombre);
		ReglasDeLaEstructura.consecutivosSinRepetir(items.stream().map(ItemDePlantilla::consecutivo).toList(), donde);
		ReglasDeLaEstructura.sumanExactamente100(items.stream().map(ItemDePlantilla::porcentaje).toList(), donde);
	}
}
