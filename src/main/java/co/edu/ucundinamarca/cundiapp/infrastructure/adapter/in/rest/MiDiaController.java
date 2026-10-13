package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiDia;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.MiDiaDto;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La pantalla del día (RF07, SCRUM-31 y SCRUM-32): lo que le toca al estudiante hoy, o el día que pida,
 * con la clase próxima destacada. Sin fecha es hoy en Bogotá.
 */
@RestController
@RequestMapping("/api/mis/dia")
class MiDiaController {

	private final ConsultarMiDia consultar;

	MiDiaController(ConsultarMiDia consultar) {
		this.consultar = consultar;
	}

	@GetMapping
	MiDiaDto dia(
			@AuthenticationPrincipal Jwt token,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return MiDiaDto.desde(consultar.ejecutar(Integer.parseInt(token.getSubject()), fecha));
	}
}
