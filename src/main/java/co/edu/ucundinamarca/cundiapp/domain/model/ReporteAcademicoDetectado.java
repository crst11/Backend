package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.List;

/**
 * Lo que un Registro Académico Extendido dice, ya leído y todavía sin guardar (RF03, SCRUM-23).
 *
 * <p>Esto es lo que se le muestra al estudiante para que confirme antes de que nada toque su
 * historial. No incluye su documento de identidad: la Ley 1581 prohíbe almacenarlo, así que el
 * lector ni siquiera lo saca del archivo.
 */
public record ReporteAcademicoDetectado(
		String programa,
		String sede,
		List<PeriodoDetectado> periodos,
		List<ResumenOficialDePeriodo> oficiales) {

	public ReporteAcademicoDetectado {
		if (periodos == null || periodos.isEmpty()) {
			throw new ReglaDeNegocioVioladaException(
					"El reporte no trae ningún período con asignaturas");
		}
		periodos = List.copyOf(periodos);
		oficiales = oficiales == null ? List.of() : List.copyOf(oficiales);
	}

	public int totalDeAsignaturas() {
		return periodos.stream().mapToInt(periodo -> periodo.asignaturas().size()).sum();
	}
}
