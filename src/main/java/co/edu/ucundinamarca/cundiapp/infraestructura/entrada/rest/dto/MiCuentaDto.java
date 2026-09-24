package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;

public record MiCuentaDto(Integer id, String correo, String nombres, String apellidos, String estado) {

	public static MiCuentaDto desde(Estudiante estudiante) {
		return new MiCuentaDto(
				estudiante.id(),
				estudiante.correo().valor(),
				estudiante.nombres(),
				estudiante.apellidos(),
				estudiante.estado().name().toLowerCase());
	}
}
