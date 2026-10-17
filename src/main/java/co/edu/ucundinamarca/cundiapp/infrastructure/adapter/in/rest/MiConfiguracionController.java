package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMiMeta;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CambiarMetaDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ConfiguracionDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Los ajustes propios del estudiante (RF08, SCRUM-34). El estudiante sale del token. */
@RestController
@RequestMapping("/api/mis/configuracion")
class MiConfiguracionController {

	private final ConsultarMiConfiguracion consultar;
	private final CambiarMiMeta cambiarMeta;

	MiConfiguracionController(ConsultarMiConfiguracion consultar, CambiarMiMeta cambiarMeta) {
		this.consultar = consultar;
		this.cambiarMeta = cambiarMeta;
	}

	@GetMapping
	ConfiguracionDto consultar(@AuthenticationPrincipal Jwt token) {
		return ConfiguracionDto.desde(consultar.ejecutar(Integer.parseInt(token.getSubject())));
	}

	@PatchMapping
	ConfiguracionDto cambiar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CambiarMetaDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		return ConfiguracionDto.desde(cambiarMeta.ejecutar(idEstudiante, new Meta(datos.meta())));
	}
}
