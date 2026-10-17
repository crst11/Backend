package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMisPreferenciasDeAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMisPreferenciasDeAvisos;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.PreferenciasDeAvisosDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configurar mis avisos (RF10, SCRUM-38): qué tipos recibo y con cuánta anticipación. Se guarda todo junto
 * con PUT, porque un campo suelto que falte no debe interpretarse como «apagado». El estudiante sale del token.
 */
@RestController
@RequestMapping("/api/mis/configuracion/avisos")
class MisPreferenciasDeAvisosController {

	private final ConsultarMisPreferenciasDeAvisos consultar;
	private final CambiarMisPreferenciasDeAvisos cambiar;

	MisPreferenciasDeAvisosController(ConsultarMisPreferenciasDeAvisos consultar, CambiarMisPreferenciasDeAvisos cambiar) {
		this.consultar = consultar;
		this.cambiar = cambiar;
	}

	@GetMapping
	PreferenciasDeAvisosDto consultar(@AuthenticationPrincipal Jwt token) {
		return PreferenciasDeAvisosDto.desde(consultar.ejecutar(Integer.parseInt(token.getSubject())));
	}

	@PutMapping
	PreferenciasDeAvisosDto cambiar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody PreferenciasDeAvisosDto datos) {
		return PreferenciasDeAvisosDto.desde(cambiar.ejecutar(Integer.parseInt(token.getSubject()), datos.aPreferencias()));
	}
}
