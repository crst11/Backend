package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Una asignatura que el estudiante matriculó en un período, con la nota con que quedó (RF02).
 *
 * <p>La nota definitiva puede venir nula: mientras el período está en curso todavía no existe, y
 * contarla como 0.0 bajaría el promedio con una nota que nadie puso. Los créditos pueden ser cero:
 * las de diagnóstico y nivelatorio se cursan y se califican, pero no ponderan.
 */
public record AsignaturaCursada(
		String codigo,
		String nombre,
		int creditos,
		BigDecimal notaDefinitiva,
		EstadoDeAsignatura estado) {

	private static final BigDecimal NOTA_MINIMA = BigDecimal.ZERO;
	private static final BigDecimal NOTA_MAXIMA = new BigDecimal("5.0");

	public AsignaturaCursada {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("La asignatura cursada necesita un código");
		}
		if (creditos < 0) {
			throw new ReglaDeNegocioVioladaException("Los créditos de una asignatura no pueden ser negativos");
		}
		if (estado == null) {
			throw new ReglaDeNegocioVioladaException("La asignatura cursada necesita un estado");
		}
		if (notaDefinitiva != null
				&& (notaDefinitiva.compareTo(NOTA_MINIMA) < 0 || notaDefinitiva.compareTo(NOTA_MAXIMA) > 0)) {
			throw new ReglaDeNegocioVioladaException("La nota debe estar entre 0.0 y 5.0");
		}
	}

	public Optional<BigDecimal> nota() {
		return Optional.ofNullable(notaDefinitiva);
	}

	/** Pondera solo lo que tiene nota y vale créditos, y siempre que no se haya cancelado. */
	public boolean ponderaEnElPromedio() {
		return creditos > 0 && notaDefinitiva != null && estado.cuentaEnLaCarga();
	}

	public boolean estaAprobada() {
		return estado == EstadoDeAsignatura.APROBADA;
	}
}
