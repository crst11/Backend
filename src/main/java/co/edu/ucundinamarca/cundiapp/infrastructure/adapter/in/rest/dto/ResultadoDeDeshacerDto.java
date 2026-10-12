package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.DeshacerImportacion.Resultado;

/**
 * Lo que se devolvió al deshacer (SCRUM-24): `eliminadas` son las que esa carga había creado y
 * `restauradas` las que había pisado y volvieron a su valor anterior.
 */
public record ResultadoDeDeshacerDto(int idImportacion, int eliminadas, int restauradas) {

	public static ResultadoDeDeshacerDto desde(Resultado resultado) {
		return new ResultadoDeDeshacerDto(
				resultado.idImportacion(), resultado.eliminadas(), resultado.restauradas());
	}
}
