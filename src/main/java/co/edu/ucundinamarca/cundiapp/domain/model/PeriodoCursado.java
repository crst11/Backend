package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Un período académico ya matriculado, con las asignaturas que el estudiante vio en él (RF02).
 *
 * <p>El promedio del período es ponderado por créditos, como lo calcula la universidad: una
 * asignatura de 4 créditos pesa el doble que una de 2. El promedio simple de las notas daría otro
 * número y no coincidiría con el reporte oficial.
 */
public record PeriodoCursado(String codigo, List<AsignaturaCursada> asignaturas) {

	public PeriodoCursado {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El período necesita un código");
		}
		asignaturas = asignaturas == null ? List.of() : List.copyOf(asignaturas);
	}

	/** Lo que el estudiante lleva encima este período: no incluye lo que canceló. */
	public int creditosMatriculados() {
		return asignaturas.stream()
				.filter(asignatura -> asignatura.estado().cuentaEnLaCarga())
				.mapToInt(AsignaturaCursada::creditos)
				.sum();
	}

	public int creditosAprobados() {
		return asignaturas.stream()
				.filter(AsignaturaCursada::estaAprobada)
				.mapToInt(AsignaturaCursada::creditos)
				.sum();
	}

	/** Vacío mientras no haya ninguna nota: un período recién matriculado no tiene promedio, y 0.0 mentiría. */
	public Optional<BigDecimal> promedio() {
		return PromedioPonderado.de(asignaturas);
	}

	int creditosQuePonderan() {
		return PromedioPonderado.creditos(asignaturas);
	}

	BigDecimal puntosPonderados() {
		return PromedioPonderado.puntos(asignaturas);
	}
}
