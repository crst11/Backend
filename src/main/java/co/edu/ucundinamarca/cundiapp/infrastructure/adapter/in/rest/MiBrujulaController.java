package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiBrujula;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.BrujulaDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La Brújula Académica (RF09, SCRUM-35): el riesgo de cada asignatura en curso. Solo lee; el estudiante
 * sale del token.
 */
@RestController
@RequestMapping("/api/mis/brujula")
class MiBrujulaController {

	private final ConsultarMiBrujula brujula;

	MiBrujulaController(ConsultarMiBrujula brujula) {
		this.brujula = brujula;
	}

	@GetMapping
	BrujulaDto consultar(@AuthenticationPrincipal Jwt token) {
		return BrujulaDto.desde(brujula.ejecutar(Integer.parseInt(token.getSubject())));
	}
}
