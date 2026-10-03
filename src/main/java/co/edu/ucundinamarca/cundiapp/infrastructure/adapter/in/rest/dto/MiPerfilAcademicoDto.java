package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.MiPerfilAcademico;

/** El programa que el estudiante eligió. `plan` viene nulo mientras no elija (SCRUM-21). */
public record MiPerfilAcademicoDto(boolean programaElegido, PlanDeEstudiosDto plan) {

	public static MiPerfilAcademicoDto desde(MiPerfilAcademico perfil) {
		return perfil.plan()
				.map(plan -> new MiPerfilAcademicoDto(true, PlanDeEstudiosDto.desde(plan)))
				.orElseGet(() -> new MiPerfilAcademicoDto(false, null));
	}
}
