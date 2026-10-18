package co.edu.ucundinamarca.cundiapp.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Qué nota necesita el estudiante en lo que falta para llegar a su meta (RF08, SCRUM-33 y 34).
 *
 * <p>No se guarda: depende de la estructura, de las notas y de la meta, y cualquiera de las tres
 * puede cambiar. Las notas simuladas (SCRUM-34) producen la misma proyección que las reales, y por
 * eso nunca se escriben.
 *
 * @param meta la meta con la que se hizo la cuenta
 * @param puntos los puntos ganados hasta ahora sobre 5.0
 * @param porcentajeEvaluado cuánto de la nota final ya tiene nota, de 0 a 100
 * @param porcentajeRestante cuánto falta por evaluar, de 0 a 100
 * @param notaRequerida el promedio que necesita en lo que falta, redondeado hacia arriba a dos
 *     decimales para no decir que basta con una nota con la que en realidad no se llega; 0.00 si ya
 *     alcanzó la meta y vacío si ya no la puede alcanzar
 * @param maximoPosible lo más alto que puede terminar la asignatura: lo que lleva más 5.0 en todo lo
 *     que falta
 * @param situacion si la meta ya se alcanzó, se puede alcanzar o ya no
 */
public record ProyeccionDeNota(
		Meta meta,
		BigDecimal puntos,
		BigDecimal porcentajeEvaluado,
		BigDecimal porcentajeRestante,
		BigDecimal notaRequerida,
		BigDecimal maximoPosible,
		SituacionDeLaMeta situacion) {

	public Optional<BigDecimal> requerida() {
		return Optional.ofNullable(notaRequerida);
	}
}
