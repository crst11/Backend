package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;

/**
 * Una asignatura leída de un reporte institucional, antes de confirmarse (RF03, SCRUM-23).
 *
 * <p>Solo trae el código y la nota: el nombre y los créditos salen de la ruta de aprendizaje que
 * ya está en la base, que es la fuente oficial. Sacarlos del PDF sería frágil y redundante.
 */
public record AsignaturaDetectada(String codigo, BigDecimal notaDefinitiva) {

	/** Nota mínima para aprobar, según el Reglamento Estudiantil. */
	public static final BigDecimal NOTA_MINIMA_APROBATORIA = new BigDecimal("3.0");

	public AsignaturaDetectada {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("La asignatura detectada necesita un código");
		}
	}

	public boolean aprobada() {
		return notaDefinitiva != null && notaDefinitiva.compareTo(NOTA_MINIMA_APROBATORIA) >= 0;
	}

	public EstadoDeAsignatura estado() {
		if (notaDefinitiva == null) {
			return EstadoDeAsignatura.EN_CURSO;
		}
		return aprobada() ? EstadoDeAsignatura.APROBADA : EstadoDeAsignatura.REPROBADA;
	}
}
