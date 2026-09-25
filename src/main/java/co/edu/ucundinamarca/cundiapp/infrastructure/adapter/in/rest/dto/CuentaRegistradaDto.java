package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

public record CuentaRegistradaDto(Integer id, String correo, String estado) {

	public static CuentaRegistradaDto desde(Estudiante estudiante) {
		return new CuentaRegistradaDto(
				estudiante.id(), estudiante.correo().valor(), estudiante.estado().name().toLowerCase());
	}
}
