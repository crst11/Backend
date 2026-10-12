package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.List;

/** Un período leído de un reporte institucional, antes de confirmarse (RF03, SCRUM-23). */
public record PeriodoDetectado(String codigo, List<AsignaturaDetectada> asignaturas) {

	public PeriodoDetectado {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El período detectado necesita un código");
		}
		asignaturas = asignaturas == null ? List.of() : List.copyOf(asignaturas);
	}
}
