package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Una categoría de la guía institucional (RF11): agrupa los recursos que se publican sin
 * necesidad de cuenta (reglamentos, formatos, convocatorias).
 */
public record CategoriaDeRecurso(Integer id, String nombre) {

	public CategoriaDeRecurso {
		if (nombre == null || nombre.isBlank()) {
			throw new ReglaDeNegocioVioladaException("La categoría necesita un nombre");
		}
	}
}
