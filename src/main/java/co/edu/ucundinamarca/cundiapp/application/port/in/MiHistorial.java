package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.HistorialAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.ResumenOficialDePeriodo;
import java.util.List;
import java.util.Optional;

/**
 * El historial del estudiante junto con lo que hace falta para leerlo: los promedios oficiales que
 * trajo su reporte y el programa contra el que se mide el avance (SCRUM-22).
 *
 * <p>El programa viene vacío mientras el estudiante no haya elegido uno: sin él hay notas y
 * promedios, pero no hay contra qué medir el porcentaje de avance.
 */
public record MiHistorial(
		HistorialAcademico historial,
		List<ResumenOficialDePeriodo> oficiales,
		ProgramaAcademico programa) {

	public MiHistorial {
		oficiales = oficiales == null ? List.of() : List.copyOf(oficiales);
	}

	public Optional<ProgramaAcademico> programaElegido() {
		return Optional.ofNullable(programa);
	}

	public Optional<ResumenOficialDePeriodo> oficialDe(String codigoPeriodo) {
		return oficiales.stream().filter(r -> r.codigoPeriodo().equals(codigoPeriodo)).findFirst();
	}
}
