package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;

/**
 * La nota de una actividad: de 0.0 a 5.0, que es la escala de la universidad (RF05, SCRUM-28).
 *
 * <p>Admite hasta dos decimales porque es lo que guarda la base de datos (NUMERIC(3,2)) y lo que
 * usa Academusoft internamente, aunque lo imprima con uno. Se compara por valor, no por formato:
 * 4.500 es 4.5 y no se rechaza por traer un cero de más.
 */
public record Nota(BigDecimal valor) {

	private static final BigDecimal MINIMA = BigDecimal.ZERO;
	private static final BigDecimal MAXIMA = new BigDecimal("5");
	private static final int DECIMALES = 2;

	public Nota {
		if (valor == null) {
			throw new ReglaDeNegocioVioladaException("Falta la nota");
		}
		if (valor.compareTo(MINIMA) < 0 || valor.compareTo(MAXIMA) > 0) {
			throw new ReglaDeNegocioVioladaException("La nota tiene que estar entre 0.0 y 5.0");
		}
		if (valor.stripTrailingZeros().scale() > DECIMALES) {
			throw new ReglaDeNegocioVioladaException("La nota admite como máximo dos decimales");
		}
	}
}
