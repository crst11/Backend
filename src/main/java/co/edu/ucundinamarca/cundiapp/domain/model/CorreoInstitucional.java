package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/** Solo se acepta el dominio de correo de la Universidad de Cundinamarca. */
public record CorreoInstitucional(String valor) {

	private static final String DOMINIO = "@ucundinamarca.edu.co";

	public CorreoInstitucional {
		if (valor == null || valor.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El correo institucional es obligatorio");
		}
		valor = valor.trim();
		if (!valor.toLowerCase().endsWith(DOMINIO)) {
			throw new ReglaDeNegocioVioladaException(
					"El correo debe pertenecer al dominio institucional " + DOMINIO);
		}
	}
}
