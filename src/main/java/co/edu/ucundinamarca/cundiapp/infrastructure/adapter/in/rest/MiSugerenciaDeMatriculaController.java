package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.SugerirMatricula;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.SugerenciaDeMatriculaDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Las materias que conviene matricular el próximo semestre (RF13, SCRUM-244). Solo lectura. */
@RestController
@RequestMapping("/api/mis/horario/sugerencia-de-matricula")
class MiSugerenciaDeMatriculaController {

	private final SugerirMatricula sugerirMatricula;

	MiSugerenciaDeMatriculaController(SugerirMatricula sugerirMatricula) {
		this.sugerirMatricula = sugerirMatricula;
	}

	@GetMapping
	SugerenciaDeMatriculaDto sugerencia(@AuthenticationPrincipal Jwt token) {
		return SugerenciaDeMatriculaDto.desde(sugerirMatricula.ejecutar(Integer.parseInt(token.getSubject())));
	}
}
