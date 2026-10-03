package co.edu.ucundinamarca.cundiapp.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * La cuenta del promedio ponderado por créditos, en un solo lugar para que el período y el
 * acumulado no se calculen de dos formas distintas.
 *
 * <p>Se expone con dos decimales porque esa es la precisión con que la universidad guarda las
 * notas (y el esquema del proyecto, con NUMERIC(3,2)). Academusoft las publica con uno, así que el
 * acumulado del reporte no se puede reproducir al último decimal; redondear aquí a un decimal solo
 * escondería esa diferencia detrás de una regla inventada.
 */
final class PromedioPonderado {

	private static final int DECIMALES = 2;

	private PromedioPonderado() {
	}

	static Optional<BigDecimal> de(List<AsignaturaCursada> asignaturas) {
		int creditos = creditos(asignaturas);
		if (creditos == 0) {
			return Optional.empty();
		}
		return Optional.of(dividir(puntos(asignaturas), creditos));
	}

	static BigDecimal dividir(BigDecimal puntos, int creditos) {
		return puntos.divide(BigDecimal.valueOf(creditos), DECIMALES, RoundingMode.HALF_UP);
	}

	static int creditos(List<AsignaturaCursada> asignaturas) {
		return asignaturas.stream()
				.filter(AsignaturaCursada::ponderaEnElPromedio)
				.mapToInt(AsignaturaCursada::creditos)
				.sum();
	}

	static BigDecimal puntos(List<AsignaturaCursada> asignaturas) {
		return asignaturas.stream()
				.filter(AsignaturaCursada::ponderaEnElPromedio)
				.map(asignatura -> asignatura.notaDefinitiva().multiply(BigDecimal.valueOf(asignatura.creditos())))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.round(MathContext.DECIMAL64);
	}
}
