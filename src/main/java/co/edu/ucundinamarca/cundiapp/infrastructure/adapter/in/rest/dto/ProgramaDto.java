package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;

/** Un programa entre los que el estudiante puede elegir (SCRUM-21). */
public record ProgramaDto(
		String codigo, String nombre, String facultad, String sede, int totalCreditos, int numeroPeriodos) {

	public static ProgramaDto desde(ProgramaAcademico programa) {
		return new ProgramaDto(programa.codigo(), programa.nombre(), programa.facultad(), programa.sede(),
				programa.totalCreditos(), programa.numeroPeriodos());
	}
}
