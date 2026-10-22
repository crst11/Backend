package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.VerificarCambioDeGrupo;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.VerificacionesDeHorarioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.VerificarCambioDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cambiar un grupo del horario armado (RF13, SCRUM-245): la persona manda su horario y los otros grupos de una
 * materia, y recibe por cada grupo si cabe o con qué clase choca. No guarda nada: es un cálculo.
 *
 * <p>Va con POST porque lleva un cuerpo, pero no cambia nada del servidor. Sin el cuerpo en el log: lo
 * escribe una persona.
 */
@RestController
@RequestMapping("/api/mis/horario/verificaciones")
class MisVerificacionesDeHorarioController {

	private final VerificarCambioDeGrupo verificar;

	MisVerificacionesDeHorarioController(VerificarCambioDeGrupo verificar) {
		this.verificar = verificar;
	}

	@PostMapping
	VerificacionesDeHorarioDto verificar(@Valid @RequestBody VerificarCambioDto datos) {
		return VerificacionesDeHorarioDto.desde(
				verificar.ejecutar(datos.aHorario(), datos.asignaturaACambiar(), datos.aCandidatos()));
	}
}
