package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiCuenta;
import co.edu.ucundinamarca.cundiapp.application.port.in.EliminarCuenta;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.MiCuentaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas del estudiante autenticado: el estudiante sale del token, nunca de un parámetro de la ruta. */
@RestController
@RequestMapping("/api/mis")
class MiCuentaController {

	private static final Logger log = LoggerFactory.getLogger(MiCuentaController.class);

	private final ConsultarMiCuenta consultarMiCuenta;
	private final EliminarCuenta eliminarCuenta;

	MiCuentaController(ConsultarMiCuenta consultarMiCuenta, EliminarCuenta eliminarCuenta) {
		this.consultarMiCuenta = consultarMiCuenta;
		this.eliminarCuenta = eliminarCuenta;
	}

	@GetMapping("/cuenta")
	MiCuentaDto cuenta(@AuthenticationPrincipal Jwt token) {
		return MiCuentaDto.desde(consultarMiCuenta.ejecutar(Integer.parseInt(token.getSubject())));
	}

	@DeleteMapping("/cuenta")
	ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt token) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		eliminarCuenta.ejecutar(idEstudiante);
		log.info("Cuenta eliminada: estudiante {}", idEstudiante);
		return ResponseEntity.noContent().build();
	}
}
