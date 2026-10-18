package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.CambiarEstadoDeMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.in.CrearMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.in.EliminarMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisPendientes;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CambiarEstadoDePendienteDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CrearPendienteDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.PendienteDto;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mis pendientes (RF06, SCRUM-29 y SCRUM-30). Como en el resto de {@code /api/mis}, el estudiante sale del
 * token y un pendiente ajeno responde igual que uno que no existe.
 */
@RestController
@RequestMapping("/api/mis/pendientes")
class MisPendientesController {

	private static final Logger log = LoggerFactory.getLogger(MisPendientesController.class);

	private final CrearMiPendiente crear;
	private final ListarMisPendientes listar;
	private final CambiarEstadoDeMiPendiente cambiarEstado;
	private final EliminarMiPendiente eliminar;

	MisPendientesController(
			CrearMiPendiente crear,
			ListarMisPendientes listar,
			CambiarEstadoDeMiPendiente cambiarEstado,
			EliminarMiPendiente eliminar) {
		this.crear = crear;
		this.listar = listar;
		this.cambiarEstado = cambiarEstado;
		this.eliminar = eliminar;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	PendienteDto crear(@AuthenticationPrincipal Jwt token, @RequestBody CrearPendienteDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var creado = crear.ejecutar(idEstudiante, datos.aDatos());
		// Sin el título en el log: lo escribe una persona y puede nombrar a otras.
		log.info("Pendiente creado: estudiante {}, pendiente {}", idEstudiante, creado.pendiente().id());
		return PendienteDto.desde(creado);
	}

	/** Del más próximo al más lejano; sin los cumplidos, a menos que se pidan con incluirCumplidos=true. */
	@GetMapping
	List<PendienteDto> listar(
			@AuthenticationPrincipal Jwt token, @RequestParam(defaultValue = "false") boolean incluirCumplidos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		return listar.ejecutar(idEstudiante, incluirCumplidos).stream().map(PendienteDto::desde).toList();
	}

	@PatchMapping("/{idPendiente}")
	PendienteDto cambiarEstado(
			@AuthenticationPrincipal Jwt token,
			@PathVariable int idPendiente,
			@Valid @RequestBody CambiarEstadoDePendienteDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var cambiado = cambiarEstado.ejecutar(idEstudiante, idPendiente, datos.cumplido());
		log.info("Pendiente {}: estudiante {}, pendiente {}", datos.cumplido() ? "cumplido" : "reabierto",
				idEstudiante, idPendiente);
		return PendienteDto.desde(cambiado);
	}

	@DeleteMapping("/{idPendiente}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void eliminar(@AuthenticationPrincipal Jwt token, @PathVariable int idPendiente) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		eliminar.ejecutar(idEstudiante, idPendiente);
		log.info("Pendiente eliminado: estudiante {}, pendiente {}", idEstudiante, idPendiente);
	}
}
