package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.List;

/**
 * Una asignatura del plan, con el período en que la ruta sugiere cursarla y los códigos de las que
 * hay que aprobar antes (RF02).
 *
 * <p>Los créditos pueden ser cero: las de diagnóstico y nivelatorio se cursan pero no ponderan.
 */
public record Asignatura(
		String codigo,
		String nombre,
		int creditos,
		TipoDeAsignatura tipo,
		Integer periodoSugerido,
		List<String> prerrequisitos) {

	public Asignatura {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("La asignatura necesita un código");
		}
		if (creditos < 0) {
			throw new ReglaDeNegocioVioladaException("Los créditos de una asignatura no pueden ser negativos");
		}
		prerrequisitos = prerrequisitos == null ? List.of() : List.copyOf(prerrequisitos);
	}

	/** Las de diagnóstico y nivelatorio: se cursan, pero no suman al avance ni al promedio. */
	public boolean pondera() {
		return creditos > 0;
	}
}
