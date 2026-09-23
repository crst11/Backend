package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

public record CuentaRegistradaDto(Integer id, String correo, String estado) {

	public static CuentaRegistradaDto desde(Estudiante estudiante) {
		return new CuentaRegistradaDto(
				estudiante.id(), estudiante.correo().valor(), estudiante.estado().name().toLowerCase());
	}
}
