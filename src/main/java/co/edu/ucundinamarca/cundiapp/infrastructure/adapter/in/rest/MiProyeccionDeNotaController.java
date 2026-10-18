package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ProyeccionDeNotaDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.SimularNotasDto;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La nota que necesito y el simulador (RF08, SCRUM-33 y SCRUM-34).
 *
 * <p>Las dos rutas solo leen. {@code POST /simulaciones} es un POST porque lleva cuerpo, no porque
 * escriba: nada de lo que se simula se guarda. Como en el resto de {@code /api/mis}, el estudiante
 * sale del token y una matrícula ajena responde «no encontramos esa asignatura».
 */
@RestController
@RequestMapping("/api/mis/matriculas/{idMatricula}")
class MiProyeccionDeNotaController {

	private final ConsultarMiProyeccionDeNota proyeccion;
	private final SimularMiNota simulacion;

	MiProyeccionDeNotaController(ConsultarMiProyeccionDeNota proyeccion, SimularMiNota simulacion) {
		this.proyeccion = proyeccion;
		this.simulacion = simulacion;
	}

	/** Con la meta guardada, o con la de {@code ?meta=} si viene (sin guardarla). */
	@GetMapping("/proyeccion")
	ProyeccionDeNotaDto proyeccion(
			@AuthenticationPrincipal Jwt token,
			@PathVariable int idMatricula,
			@RequestParam(required = false) BigDecimal meta) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		Meta aProbar = meta == null ? null : new Meta(meta);
		return ProyeccionDeNotaDto.desde(proyeccion.ejecutar(idEstudiante, idMatricula, aProbar));
	}

	@PostMapping("/simulaciones")
	ProyeccionDeNotaDto simular(
			@AuthenticationPrincipal Jwt token,
			@PathVariable int idMatricula,
			@Valid @RequestBody SimularNotasDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		return ProyeccionDeNotaDto.desde(
				simulacion.ejecutar(idEstudiante, idMatricula, datos.aMeta(), datos.aNotasSimuladas()));
	}
}
