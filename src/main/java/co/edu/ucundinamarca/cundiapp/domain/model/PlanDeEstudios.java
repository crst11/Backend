package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * La ruta de aprendizaje de un programa: qué asignaturas lo componen y en qué período se sugiere
 * cursar cada una (RF02).
 *
 * <p>Agrupar por período vive aquí y no en el controlador ni en la pantalla: es cómo se lee un plan
 * de estudios, no cómo se dibuja.
 */
public record PlanDeEstudios(String codigo, ProgramaAcademico programa, List<Asignatura> asignaturas) {

	public PlanDeEstudios {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El plan necesita un código");
		}
		asignaturas = asignaturas == null ? List.of() : List.copyOf(asignaturas);
	}

	/**
	 * Ordena en español, no por código de carácter: comparando sin más, la "Á" de Álgebra Lineal cae
	 * después de la "D" de Diagnóstico, y una lista así se ve mal.
	 */
	private static final Comparator<String> ALFABETICO_EN_ESPANOL = Collator.getInstance(Locale.of("es"))::compare;

	/** Las asignaturas por período, de menor a mayor y en orden alfabético dentro de cada uno. */
	public Map<Integer, List<Asignatura>> porPeriodo() {
		return asignaturas.stream()
				.filter(asignatura -> asignatura.periodoSugerido() != null)
				.sorted(Comparator.comparing(Asignatura::periodoSugerido)
						.thenComparing(Asignatura::nombre, ALFABETICO_EN_ESPANOL))
				.collect(Collectors.groupingBy(
						Asignatura::periodoSugerido, LinkedHashMap::new, Collectors.toList()));
	}

	/** Lo que suma el plan. Debe coincidir con el total que declara el programa. */
	public int creditosTotales() {
		return asignaturas.stream().mapToInt(Asignatura::creditos).sum();
	}

	public int creditosDelPeriodo(int periodo) {
		return asignaturas.stream()
				.filter(asignatura -> Integer.valueOf(periodo).equals(asignatura.periodoSugerido()))
				.mapToInt(Asignatura::creditos)
				.sum();
	}
}
