package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Un programa de pregrado en una sede (RF02). La misma carrera en dos sedes son dos programas: tienen
 * código SNIES distinto y pueden tener rutas de aprendizaje distintas.
 */
public record ProgramaAcademico(
		String codigo,
		String nombre,
		String facultad,
		String sede,
		int totalCreditos,
		int numeroPeriodos) {

	public ProgramaAcademico {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El programa necesita un código");
		}
		if (totalCreditos <= 0) {
			throw new ReglaDeNegocioVioladaException("El programa necesita un total de créditos mayor que cero");
		}
		if (numeroPeriodos <= 0) {
			throw new ReglaDeNegocioVioladaException("El programa necesita al menos un período");
		}
	}
}
