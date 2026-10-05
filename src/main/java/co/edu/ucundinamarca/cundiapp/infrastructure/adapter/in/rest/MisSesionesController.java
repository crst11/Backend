package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.CerrarSesionEnDispositivo;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisSesiones;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.DispositivoConSesionDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ver y cerrar las sesiones abiertas del estudiante (RF01, SCRUM-49). */
@RestController
@RequestMapping("/api/mis/sesiones")
class MisSesionesController {

	private static final Logger log = LoggerFactory.getLogger(MisSesionesController.class);

	private final ListarMisSesiones listarMisSesiones;
	private final CerrarSesionEnDispositivo cerrarSesion;

	MisSesionesController(ListarMisSesiones listarMisSesiones, CerrarSesionEnDispositivo cerrarSesion) {
		this.listarMisSesiones = listarMisSesiones;
		this.cerrarSesion = cerrarSesion;
	}

	@GetMapping
	List<DispositivoConSesionDto> abiertas(@AuthenticationPrincipal Jwt token) {
		return listarMisSesiones.ejecutar(Integer.parseInt(token.getSubject()), sesionDe(token)).stream()
				.map(DispositivoConSesionDto::desde)
				.toList();
	}

	/**
	 * A qué sesión pertenece el token de quien pregunta, para marcar su dispositivo como el actual.
	 * El claim lo pone EmisorDeTokensJwt; los tokens emitidos antes de SCRUM-73 no lo traen y
	 * entonces no se marca ninguno, que es mejor que marcar el equivocado.
	 */
	private static Integer sesionDe(Jwt token) {
		return token.getClaim("sid") instanceof Number consecutivo ? consecutivo.intValue() : null;
	}

	@DeleteMapping("/{consecutivo}")
	ResponseEntity<Void> cerrarUna(@AuthenticationPrincipal Jwt token, @PathVariable int consecutivo) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		cerrarSesion.ejecutar(idEstudiante, consecutivo);
		log.info("Sesión {} cerrada desde Mis sesiones: estudiante {}", consecutivo, idEstudiante);
		return ResponseEntity.noContent().build();
	}

	/** Cierra todas, incluida la que hace la petición: el frontend limpia su sesión y vuelve a la entrada. */
	@DeleteMapping
	ResponseEntity<Void> cerrarTodas(@AuthenticationPrincipal Jwt token) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		cerrarSesion.ejecutarTodas(idEstudiante);
		log.info("Todas las sesiones cerradas desde Mis sesiones: estudiante {}", idEstudiante);
		return ResponseEntity.noContent().build();
	}
}
