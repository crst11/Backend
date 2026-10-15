package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiAvanceDeInformacion;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AvanceDeInformacionDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cuánta información lleva cargada el estudiante y qué le falta (RF03, SCRUM-84). */
@RestController
@RequestMapping("/api/mis/avance-de-informacion")
class MiAvanceDeInformacionController {

	private final ConsultarMiAvanceDeInformacion consultar;

	MiAvanceDeInformacionController(ConsultarMiAvanceDeInformacion consultar) {
		this.consultar = consultar;
	}

	@GetMapping
	AvanceDeInformacionDto avance(@AuthenticationPrincipal Jwt token) {
		return AvanceDeInformacionDto.desde(consultar.ejecutar(Integer.parseInt(token.getSubject())));
	}
}
