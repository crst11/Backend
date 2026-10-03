package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarPlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarProgramas;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.PlanDeEstudiosDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ProgramaDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de programas y sus rutas de aprendizaje (RF02, SCRUM-21).
 *
 * <p>No va bajo `/api/mis/**` porque no es información del estudiante: es el catálogo con el que
 * elige. Exige sesión igual que el resto.
 */
@RestController
@RequestMapping("/api/programas")
class ProgramasController {

	private final ListarProgramas listarProgramas;
	private final ConsultarPlanDeEstudios consultarPlan;

	ProgramasController(ListarProgramas listarProgramas, ConsultarPlanDeEstudios consultarPlan) {
		this.listarProgramas = listarProgramas;
		this.consultarPlan = consultarPlan;
	}

	@GetMapping
	List<ProgramaDto> programas() {
		return listarProgramas.ejecutar().stream().map(ProgramaDto::desde).toList();
	}

	@GetMapping("/{codigo}/plan")
	PlanDeEstudiosDto plan(@PathVariable String codigo) {
		return PlanDeEstudiosDto.desde(consultarPlan.ejecutar(codigo));
	}
}
