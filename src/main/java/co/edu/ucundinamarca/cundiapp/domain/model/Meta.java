package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;

/**
 * La nota con la que el estudiante da por buena una asignatura (RF08, SCRUM-33). La universidad
 * aprueba con 3.0, que es el valor por defecto, pero el estudiante puede apuntar más alto.
 *
 * <p>Escala de 0.0 a 5.0 y hasta dos decimales, igual que una {@link Nota} y que la columna
 * {@code configuracion_estudiante.meta_calificacion}.
 */
public record Meta(BigDecimal valor) {

	private static final BigDecimal MAXIMA = new BigDecimal("5");
	private static final int DECIMALES = 2;

	/** Va después de las constantes: construirla antes dejaría a {@code MAXIMA} sin inicializar. */
	public static final Meta PREDETERMINADA = new Meta(new BigDecimal("3.00"));

	public Meta {
		if (valor == null) {
			throw new ReglaDeNegocioVioladaException("Falta la meta");
		}
		if (valor.signum() < 0 || valor.compareTo(MAXIMA) > 0) {
			throw new ReglaDeNegocioVioladaException("La meta tiene que estar entre 0.0 y 5.0");
		}
		if (valor.stripTrailingZeros().scale() > DECIMALES) {
			throw new ReglaDeNegocioVioladaException("La meta admite como máximo dos decimales");
		}
	}
}
