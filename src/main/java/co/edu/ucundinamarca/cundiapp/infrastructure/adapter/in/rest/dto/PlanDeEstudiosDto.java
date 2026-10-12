package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import java.util.List;
import java.util.Map;

/**
 * La ruta de aprendizaje agrupada por período, como la lee el estudiante (SCRUM-21).
 * El agrupado lo hace el dominio: aquí solo se traduce a lo que viaja por HTTP.
 */
public record PlanDeEstudiosDto(String codigo, ProgramaDto programa, List<PeriodoDto> periodos) {

	public record PeriodoDto(int periodo, int creditos, List<AsignaturaDto> asignaturas) {
	}

	public record AsignaturaDto(
			String codigo, String nombre, int creditos, String tipo, List<String> prerrequisitos) {

		static AsignaturaDto desde(Asignatura asignatura) {
			return new AsignaturaDto(asignatura.codigo(), asignatura.nombre(), asignatura.creditos(),
					asignatura.tipo().valorEnBd(), asignatura.prerrequisitos());
		}
	}

	public static PlanDeEstudiosDto desde(PlanDeEstudios plan) {
		List<PeriodoDto> periodos = plan.porPeriodo().entrySet().stream()
				.map(entrada -> new PeriodoDto(
						entrada.getKey(),
						plan.creditosDelPeriodo(entrada.getKey()),
						entrada.getValue().stream().map(AsignaturaDto::desde).toList()))
				.toList();
		return new PlanDeEstudiosDto(plan.codigo(), ProgramaDto.desde(plan.programa()), periodos);
	}
}
