package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;

/**
 * Las dos notas requeridas a partir de las cuales una asignatura pasa a riesgo medio y a riesgo alto
 * (RF09, SCRUM-35 y SCRUM-36). Por defecto 3.5 y 4.5, y las cambia el propio estudiante.
 *
 * <p>Mismas reglas que la base de datos ({@code ck_configuracion_umbrales}): cada uno entre 0.0 y 5.0
 * y el medio estrictamente menor que el alto. Con dos decimales como mucho, igual que una {@link Nota}.
 */
public record UmbralesDeRiesgo(BigDecimal medio, BigDecimal alto) {

	private static final BigDecimal MAXIMA = new BigDecimal("5");
	private static final int DECIMALES = 2;

	/** Va después de las constantes: construirla antes dejaría a {@code MAXIMA} sin inicializar. */
	public static final UmbralesDeRiesgo PREDETERMINADOS =
			new UmbralesDeRiesgo(new BigDecimal("3.50"), new BigDecimal("4.50"));

	public UmbralesDeRiesgo {
		if (medio == null || alto == null) {
			throw new ReglaDeNegocioVioladaException("Faltan los umbrales de riesgo");
		}
		if (fueraDeLaEscala(medio) || fueraDeLaEscala(alto)) {
			throw new ReglaDeNegocioVioladaException("Los umbrales tienen que estar entre 0.0 y 5.0");
		}
		if (medio.stripTrailingZeros().scale() > DECIMALES || alto.stripTrailingZeros().scale() > DECIMALES) {
			throw new ReglaDeNegocioVioladaException("Los umbrales admiten como máximo dos decimales");
		}
		if (medio.compareTo(alto) >= 0) {
			throw new ReglaDeNegocioVioladaException("El umbral medio tiene que ser menor que el alto");
		}
	}

	private static boolean fueraDeLaEscala(BigDecimal valor) {
		return valor.signum() < 0 || valor.compareTo(MAXIMA) > 0;
	}
}
