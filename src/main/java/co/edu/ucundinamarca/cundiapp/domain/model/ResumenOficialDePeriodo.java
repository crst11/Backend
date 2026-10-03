package co.edu.ucundinamarca.cundiapp.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lo que la universidad publica de un período en el Registro Académico Extendido (RF02, SCRUM-22).
 *
 * <p>No es una copia de lo que calcula el motor: es el dato oficial, y cuando existe es el que ve
 * el estudiante. Academusoft imprime las notas con un decimal pero las guarda con dos, así que el
 * acumulado del reporte no se puede reproducir al último decimal; contradecirle a la universidad
 * su propia nota por una centésima que ni siquiera publica sería un error de la app, no suyo.
 */
public record ResumenOficialDePeriodo(
		String codigoPeriodo,
		int creditosMatriculados,
		int creditosAprobados,
		BigDecimal promedioPeriodo,
		BigDecimal promedioAcumulado) {

	public Optional<BigDecimal> promedioDelPeriodo() {
		return Optional.ofNullable(promedioPeriodo);
	}

	public Optional<BigDecimal> acumulado() {
		return Optional.ofNullable(promedioAcumulado);
	}
}
