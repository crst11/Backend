package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lo que la universidad publica de un período en el Registro Académico Extendido (RF02, SCRUM-22).
 *
 * <p>No es una copia de lo que calcula el motor: es el dato oficial, y cuando existe es el que ve
 * el estudiante. Academusoft imprime las notas con un decimal pero las guarda con dos, así que el
 * acumulado del reporte no se puede reproducir al último decimal; contradecirle a la universidad
 * su propia nota por una centésima que ni siquiera publica sería un error de la app, no suyo.
 *
 * <p>Los créditos se comprueban con {@link #verificarCreditos} y no al construir el registro, a propósito: el
 * registro también lo arma el lector del PDF con lo que lee del reporte, y un PDF mal leído no debe tumbar el análisis, porque
 * el estudiante puede corregir esas dos cifras en la pantalla de revisión (SCRUM-79). La comprobación se hace cuando esas
 * cifras llegan del cliente, al confirmar la importación: nadie aprueba más créditos de los que matriculó. Antes lo paraba la
 * restricción {@code ck_resumen_creditos} de la base de datos, con un 422 correcto pero genérico, sin decirle qué número arreglar.
 */
public record ResumenOficialDePeriodo(
		String codigoPeriodo,
		int creditosMatriculados,
		int creditosAprobados,
		BigDecimal promedioPeriodo,
		BigDecimal promedioAcumulado) {

	/** Lanza si los créditos de un período no cuadran, diciendo cuáles son las cifras. */
	public static void verificarCreditos(String codigoPeriodo, int creditosMatriculados, int creditosAprobados) {
		if (creditosMatriculados < 0 || creditosAprobados < 0) {
			throw new ReglaDeNegocioVioladaException(
					"Los créditos del período %s no pueden ser negativos".formatted(codigoPeriodo));
		}
		if (creditosAprobados > creditosMatriculados) {
			throw new ReglaDeNegocioVioladaException(
					"En el período %s no puedes tener %d créditos aprobados de %d matriculados: revisa esos dos números"
							.formatted(codigoPeriodo, creditosAprobados, creditosMatriculados));
		}
	}

	public Optional<BigDecimal> promedioDelPeriodo() {
		return Optional.ofNullable(promedioPeriodo);
	}

	public Optional<BigDecimal> acumulado() {
		return Optional.ofNullable(promedioAcumulado);
	}
}
