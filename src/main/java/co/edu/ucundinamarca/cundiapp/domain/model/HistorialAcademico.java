package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Todo lo que el estudiante lleva cursado, período por período (RF02, SCRUM-22).
 *
 * <p>El acumulado no es el promedio de los promedios de cada período: se calcula sobre todas las
 * asignaturas a la vez, ponderando por créditos. Promediar los promedios le daría el mismo peso a
 * un semestre de 16 créditos que a uno de 18 y no coincidiría con el reporte de la universidad.
 */
public record HistorialAcademico(List<PeriodoCursado> periodos) {

	private static final int DECIMALES_DEL_AVANCE = 2;

	public HistorialAcademico {
		periodos = periodos == null ? List.of() : periodos.stream()
				.sorted(Comparator.comparing(PeriodoCursado::codigo))
				.toList();
	}

	public Optional<BigDecimal> promedioAcumulado() {
		return promedioDe(periodos);
	}

	/** El acumulado como estaba al cerrar ese período, que es como lo muestra el reporte oficial. */
	public Optional<BigDecimal> promedioAcumuladoHasta(String codigoPeriodo) {
		List<PeriodoCursado> hasta = periodos.stream()
				.filter(periodo -> periodo.codigo().compareTo(codigoPeriodo) <= 0)
				.toList();
		return promedioDe(hasta);
	}

	public int creditosAprobados() {
		return periodos.stream().mapToInt(PeriodoCursado::creditosAprobados).sum();
	}

	/** Qué parte de la carrera lleva aprobada, de 0 a 100. */
	public BigDecimal porcentajeDeAvance(int creditosDelPrograma) {
		if (creditosDelPrograma <= 0) {
			throw new ReglaDeNegocioVioladaException(
					"No se puede calcular el avance contra un programa sin créditos");
		}
		return BigDecimal.valueOf(creditosAprobados())
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(creditosDelPrograma), DECIMALES_DEL_AVANCE, RoundingMode.HALF_UP);
	}

	private static Optional<BigDecimal> promedioDe(List<PeriodoCursado> periodos) {
		int creditos = periodos.stream().mapToInt(PeriodoCursado::creditosQuePonderan).sum();
		if (creditos == 0) {
			return Optional.empty();
		}
		BigDecimal puntos = periodos.stream()
				.map(PeriodoCursado::puntosPonderados)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return Optional.of(PromedioPonderado.dividir(puntos, creditos));
	}
}
