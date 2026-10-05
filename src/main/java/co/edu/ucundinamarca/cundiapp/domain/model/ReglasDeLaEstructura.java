package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/**
 * Las comprobaciones que comparten los dos niveles del árbol de evaluación (RF05).
 *
 * <p>Vive aparte porque la misma regla se aplica dos veces: a las categorías de una matrícula y a
 * las actividades de cada categoría. El mensaje dice cuánto falta o cuánto sobra, que es lo que el
 * estudiante necesita para corregir sin tener que hacer la cuenta él.
 */
final class ReglasDeLaEstructura {

	private static final BigDecimal CIEN = new BigDecimal("100");

	private ReglasDeLaEstructura() {
	}

	/**
	 * Compara por valor, no por formato: 50 más 50.00 son 100, aunque como texto no coincidan.
	 *
	 * @param donde cómo nombrar el conjunto en el mensaje, p. ej. "las categorías de la asignatura"
	 */
	static void sumanExactamente100(List<BigDecimal> porcentajes, String donde) {
		BigDecimal total = porcentajes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		int comparacion = total.compareTo(CIEN);
		if (comparacion == 0) {
			return;
		}
		String diferencia = comparacion < 0
				? "faltan " + enLimpio(CIEN.subtract(total))
				: "se pasan " + enLimpio(total.subtract(CIEN));
		throw new ReglaDeNegocioVioladaException("Los porcentajes de %s suman %s %%, %s %%"
				.formatted(donde, enLimpio(total), diferencia));
	}

	/** 40.00 se lee "40" y 33.30 se lee "33.3": el mensaje no es un reporte contable. */
	private static String enLimpio(BigDecimal valor) {
		return valor.stripTrailingZeros().toPlainString();
	}

	static void consecutivosSinRepetir(List<Integer> consecutivos, String donde) {
		if (new HashSet<>(consecutivos).size() != consecutivos.size()) {
			throw new ReglaDeNegocioVioladaException("Hay %s con el mismo número de orden".formatted(donde));
		}
	}

	static void porcentajeValido(BigDecimal porcentaje, String de) {
		if (porcentaje == null) {
			throw new ReglaDeNegocioVioladaException("%s necesita un porcentaje".formatted(de));
		}
		if (porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ReglaDeNegocioVioladaException(
					"El porcentaje de %s tiene que ser mayor que 0".formatted(de));
		}
		if (porcentaje.compareTo(CIEN) > 0) {
			throw new ReglaDeNegocioVioladaException(
					"El porcentaje de %s no puede pasar de 100".formatted(de));
		}
	}

	static void nombreValido(String nombre, int maximo, String de) {
		if (nombre == null || nombre.isBlank()) {
			throw new ReglaDeNegocioVioladaException("%s necesita un nombre".formatted(de));
		}
		if (nombre.length() > maximo) {
			throw new ReglaDeNegocioVioladaException(
					"El nombre de %s no puede pasar de %d caracteres".formatted(de, maximo));
		}
	}

	static void consecutivoValido(int consecutivo, String de) {
		if (consecutivo <= 0) {
			throw new ReglaDeNegocioVioladaException("El número de orden de %s empieza en 1".formatted(de));
		}
	}
}
