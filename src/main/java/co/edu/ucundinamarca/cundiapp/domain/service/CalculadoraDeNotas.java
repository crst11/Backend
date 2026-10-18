package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula la nota acumulada de una asignatura a partir de su estructura y de las notas registradas
 * (RF05, SCRUM-28). Es la base del simulador y de la Brújula, que ya tienen que partir de aquí y no
 * volver a hacer la cuenta.
 *
 * <p>El peso de una actividad en la nota final es el de su corte por el suyo dentro del corte. Todo
 * se calcula exacto y se redondea una sola vez al final: redondear cada aporte y después sumar
 * acumularía el error.
 */
public class CalculadoraDeNotas {

	private static final BigDecimal CIEN = new BigDecimal("100");
	private static final int DECIMALES = 2;

	/**
	 * Lo mismo que {@link #acumular} pero sin redondear: los puntos y el peso evaluado exactos, para
	 * que quien proyecte la nota requerida no arrastre el redondeo de la presentación.
	 *
	 * @param puntos los puntos ganados sobre 5.0, exactos
	 * @param evaluado la fracción de la nota final que ya tiene nota, de 0 a 1
	 */
	public record Exacto(BigDecimal puntos, BigDecimal evaluado, int calificadas, int totales) {
	}

	public NotaAcumulada acumular(EstructuraDeEvaluacion estructura, NotasDeLaAsignatura notas) {
		Exacto exacto = acumularExacto(estructura, notas);
		BigDecimal promedio = exacto.evaluado().signum() == 0
				? null
				: exacto.puntos().divide(exacto.evaluado(), DECIMALES, RoundingMode.HALF_UP);
		return new NotaAcumulada(
				exacto.puntos().setScale(DECIMALES, RoundingMode.HALF_UP),
				exacto.evaluado().multiply(CIEN).setScale(DECIMALES, RoundingMode.HALF_UP),
				promedio,
				exacto.calificadas(),
				exacto.totales());
	}

	public Exacto acumularExacto(EstructuraDeEvaluacion estructura, NotasDeLaAsignatura notas) {
		BigDecimal puntos = BigDecimal.ZERO;
		BigDecimal evaluado = BigDecimal.ZERO;
		int calificadas = 0;
		int totales = 0;

		for (CategoriaDeEvaluacion categoria : estructura.categorias()) {
			for (ActividadEvaluativa actividad : categoria.actividades()) {
				totales++;
				Nota nota = notas.de(categoria.consecutivo(), actividad.consecutivo()).orElse(null);
				if (nota == null) {
					continue;
				}
				calificadas++;
				// Fracción de la nota final: 30 % del corte x 70 % de la actividad = 0.21.
				BigDecimal peso = categoria.porcentaje().multiply(actividad.porcentaje())
						.divide(CIEN.multiply(CIEN));
				puntos = puntos.add(nota.valor().multiply(peso));
				evaluado = evaluado.add(peso);
			}
		}
		return new Exacto(puntos, evaluado, calificadas, totales);
	}
}
