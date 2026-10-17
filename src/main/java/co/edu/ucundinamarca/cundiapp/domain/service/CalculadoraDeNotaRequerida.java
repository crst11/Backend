package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.ProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.domain.model.SituacionDeLaMeta;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula qué nota necesita el estudiante en lo que falta para llegar a su meta (RF08, SCRUM-33 y
 * 34). Es la base de la Brújula Académica: el riesgo sale de aquí.
 *
 * <p>La cuenta es la de un promedio ponderado: lo que falta ganar (la meta menos los puntos que ya
 * lleva) repartido sobre lo que pesa lo que falta. Todo se hace exacto y se redondea una sola vez al
 * final; la nota requerida se redondea <b>hacia arriba</b>, porque redondear hacia abajo diría que
 * basta con una nota con la que no se llega.
 *
 * <p>Para simular (SCRUM-34) se le pasan las notas reales con las hipotéticas encima
 * ({@link NotasDeLaAsignatura#conSimuladas}): para la cuenta son notas como cualquier otra, y como
 * aquí no se guarda nada, lo simulado tampoco.
 */
public class CalculadoraDeNotaRequerida {

	private static final BigDecimal CIEN = new BigDecimal("100");
	private static final BigDecimal MAXIMA = new BigDecimal("5");
	private static final int DECIMALES = 2;

	private final CalculadoraDeNotas calculadora;

	public CalculadoraDeNotaRequerida(CalculadoraDeNotas calculadora) {
		this.calculadora = calculadora;
	}

	public ProyeccionDeNota proyectar(EstructuraDeEvaluacion estructura, NotasDeLaAsignatura notas, Meta meta) {
		CalculadoraDeNotas.Exacto exacto = calculadora.acumularExacto(estructura, notas);
		BigDecimal puntos = exacto.puntos();
		BigDecimal restante = BigDecimal.ONE.subtract(exacto.evaluado());
		BigDecimal maximo = puntos.add(MAXIMA.multiply(restante));

		SituacionDeLaMeta situacion;
		BigDecimal requerida;
		if (puntos.compareTo(meta.valor()) >= 0) {
			situacion = SituacionDeLaMeta.ALCANZADA;
			requerida = BigDecimal.ZERO.setScale(DECIMALES);
		} else if (maximo.compareTo(meta.valor()) < 0) {
			// Incluye el caso de que ya no quede nada por evaluar: el máximo es lo que lleva.
			situacion = SituacionDeLaMeta.INALCANZABLE;
			requerida = null;
		} else {
			situacion = SituacionDeLaMeta.ALCANZABLE;
			requerida = meta.valor().subtract(puntos).divide(restante, DECIMALES, RoundingMode.CEILING);
		}

		return new ProyeccionDeNota(
				meta,
				puntos.setScale(DECIMALES, RoundingMode.HALF_UP),
				exacto.evaluado().multiply(CIEN).setScale(DECIMALES, RoundingMode.HALF_UP),
				restante.multiply(CIEN).setScale(DECIMALES, RoundingMode.HALF_UP),
				requerida,
				maximo.setScale(DECIMALES, RoundingMode.DOWN),
				situacion);
	}
}
