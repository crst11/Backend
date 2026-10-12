package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHistorial;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.MiHistorialDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mi historial académico: notas por período, promedios y avance (RF02, SCRUM-22). */
@RestController
@RequestMapping("/api/mis/historial")
class MiHistorialController {

	private final ConsultarMiHistorial consultarHistorial;

	MiHistorialController(ConsultarMiHistorial consultarHistorial) {
		this.consultarHistorial = consultarHistorial;
	}

	@GetMapping
	MiHistorialDto historial(@AuthenticationPrincipal Jwt token) {
		return MiHistorialDto.desde(consultarHistorial.ejecutar(Integer.parseInt(token.getSubject())));
	}
}
