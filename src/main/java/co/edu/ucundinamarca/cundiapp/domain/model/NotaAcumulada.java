package co.edu.ucundinamarca.cundiapp.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lo que el estudiante lleva en una asignatura con las notas que ya registró (RF05, SCRUM-28).
 *
 * <p>No se guarda: se calcula cada vez, porque depende de la estructura y de las notas y cualquiera
 * de las dos puede cambiar.
 *
 * @param puntos los puntos ganados hasta ahora sobre 5.0: la suma de cada nota por lo que pesa en la
 *     nota final
 * @param porcentajeEvaluado cuánto de la nota final ya tiene nota, de 0 a 100
 * @param promedio lo que lleva en lo ya evaluado (puntos sobre porcentaje evaluado); vacío
 *     si todavía no hay ninguna nota, porque decir 0.0 sería inventar una nota
 * @param actividadesCalificadas cuántas actividades ya tienen nota
 * @param actividadesTotales cuántas actividades hay en total en la estructura
 */
public record NotaAcumulada(
		BigDecimal puntos,
		BigDecimal porcentajeEvaluado,
		BigDecimal promedio,
		int actividadesCalificadas,
		int actividadesTotales) {

	/** Vacío mientras no haya ninguna nota. */
	public Optional<BigDecimal> promedioDeLoEvaluado() {
		return Optional.ofNullable(promedio);
	}
}
