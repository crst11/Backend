package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ProponerHorarios;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ProponerHorariosDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.PropuestasDeHorarioDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acomodar el horario para matricular (RF13, SCRUM-208): el estudiante manda las materias con los grupos y
 * horarios que ve al matricular, y recibe combinaciones sin choques. No guarda nada: es un cálculo.
 *
 * <p>Va con POST porque lleva un cuerpo, pero no cambia nada del servidor. Sin título de materias en el log:
 * las escribe una persona.
 */
@RestController
@RequestMapping("/api/mis/horario/propuestas")
class MisPropuestasDeHorarioController {

	private final ProponerHorarios proponer;

	MisPropuestasDeHorarioController(ProponerHorarios proponer) {
		this.proponer = proponer;
	}

	@PostMapping
	PropuestasDeHorarioDto proponer(@Valid @RequestBody ProponerHorariosDto datos) {
		return PropuestasDeHorarioDto.desde(proponer.ejecutar(datos.aOferta(), datos.aCriterio()));
	}
}
