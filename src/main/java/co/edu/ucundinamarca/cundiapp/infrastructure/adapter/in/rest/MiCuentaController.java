package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiCuenta;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.MiCuentaDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas del estudiante autenticado: el estudiante sale del token, nunca de un parámetro de la ruta. */
@RestController
@RequestMapping("/api/mis")
class MiCuentaController {

	private final ConsultarMiCuenta consultarMiCuenta;

	MiCuentaController(ConsultarMiCuenta consultarMiCuenta) {
		this.consultarMiCuenta = consultarMiCuenta;
	}

	@GetMapping("/cuenta")
	MiCuentaDto cuenta(@AuthenticationPrincipal Jwt token) {
		return MiCuentaDto.desde(consultarMiCuenta.ejecutar(Integer.parseInt(token.getSubject())));
	}
}
