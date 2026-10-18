package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Qué avisos quiere recibir el estudiante y con cuánta anticipación (RF10, SCRUM-38).
 *
 * <p>El interruptor general manda sobre los de cada tipo: con `activos` en falso no sale ningún aviso,
 * aunque los demás estén encendidos. La anticipación va de una hora a una semana: cero no tiene sentido (para
 * no recibir avisos está el interruptor) y más de una semana deja de ser un recordatorio.
 */
public record PreferenciasDeAvisos(boolean activos, boolean vencimientos, boolean cambioDeRiesgo, int anticipacionEnHoras) {

	public static final int ANTICIPACION_MINIMA_EN_HORAS = 1;
	public static final int ANTICIPACION_MAXIMA_EN_HORAS = 168;

	/** Va después de las constantes: construirla antes dejaría a la validación sin sus límites. */
	public static final PreferenciasDeAvisos PREDETERMINADAS = new PreferenciasDeAvisos(true, true, true, 24);

	public PreferenciasDeAvisos {
		if (anticipacionEnHoras < ANTICIPACION_MINIMA_EN_HORAS || anticipacionEnHoras > ANTICIPACION_MAXIMA_EN_HORAS) {
			throw new ReglaDeNegocioVioladaException("La anticipación tiene que estar entre "
					+ ANTICIPACION_MINIMA_EN_HORAS + " y " + ANTICIPACION_MAXIMA_EN_HORAS + " horas");
		}
	}
}
