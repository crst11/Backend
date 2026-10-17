package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMiMeta;
import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarMisUmbrales;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiConfiguracion;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CambiarMetaDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CambiarUmbralesDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ConfiguracionDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Los ajustes propios del estudiante (RF08, SCRUM-34). El estudiante sale del token. */
@RestController
@RequestMapping("/api/mis/configuracion")
class MiConfiguracionController {

	private final ConsultarMiConfiguracion consultar;
	private final CambiarMiMeta cambiarMeta;
	private final CambiarMisUmbrales cambiarUmbrales;

	MiConfiguracionController(
			ConsultarMiConfiguracion consultar, CambiarMiMeta cambiarMeta, CambiarMisUmbrales cambiarUmbrales) {
		this.consultar = consultar;
		this.cambiarMeta = cambiarMeta;
		this.cambiarUmbrales = cambiarUmbrales;
	}

	@GetMapping
	ConfiguracionDto consultar(@AuthenticationPrincipal Jwt token) {
		return ConfiguracionDto.desde(consultar.ejecutar(Integer.parseInt(token.getSubject())));
	}

	/** Los umbrales de riesgo de la Brújula (RF09, SCRUM-36): los dos a la vez, porque uno solo no se puede validar. */
	@PutMapping("/umbrales")
	ConfiguracionDto cambiarUmbrales(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CambiarUmbralesDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		return ConfiguracionDto.desde(cambiarUmbrales.ejecutar(idEstudiante, datos.aUmbrales()));
	}

	@PatchMapping
	ConfiguracionDto cambiar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody CambiarMetaDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		return ConfiguracionDto.desde(cambiarMeta.ejecutar(idEstudiante, new Meta(datos.meta())));
	}
}
