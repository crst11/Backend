package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CalificacionRegistradaDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.EstructuraDeEvaluacionDto.AcumuladoDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.RegistrarCalificacionDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registrar mis calificaciones (RF05, SCRUM-28).
 *
 * <p>Cuelga de la actividad, no de un recurso aparte: una nota no existe sin ella, y poner la misma
 * ruta con PUT hace idempotente "anotar" y "corregir". Como en el resto de {@code /api/mis}, el
 * estudiante sale del token y una matrícula ajena responde "no encontramos esa asignatura".
 */
@RestController
@RequestMapping("/api/mis/matriculas/{idMatricula}/evaluacion/categorias/{consecCategoria}/actividades/{consecActividad}")
class MisCalificacionesController {

	private static final Logger log = LoggerFactory.getLogger(MisCalificacionesController.class);

	private final RegistrarMiCalificacion registrar;
	private final QuitarMiCalificacion quitar;

	MisCalificacionesController(RegistrarMiCalificacion registrar, QuitarMiCalificacion quitar) {
		this.registrar = registrar;
		this.quitar = quitar;
	}

	@PutMapping("/calificacion")
	CalificacionRegistradaDto registrar(
			@AuthenticationPrincipal Jwt token,
			@PathVariable int idMatricula,
			@PathVariable int consecCategoria,
			@PathVariable int consecActividad,
			@Valid @RequestBody RegistrarCalificacionDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var resultado = registrar.ejecutar(idEstudiante, idMatricula, consecCategoria, consecActividad, datos.nota());
		// Sin la nota en el log: es un dato académico de una persona.
		log.info("Calificación registrada: estudiante {}, matrícula {}", idEstudiante, idMatricula);
		return CalificacionRegistradaDto.desde(resultado);
	}

	/** Responde el acumulado que queda sin esa nota, que es lo que la pantalla necesita para redibujarse. */
	@DeleteMapping("/calificacion")
	AcumuladoDto quitar(
			@AuthenticationPrincipal Jwt token,
			@PathVariable int idMatricula,
			@PathVariable int consecCategoria,
			@PathVariable int consecActividad) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var acumulado = quitar.ejecutar(idEstudiante, idMatricula, consecCategoria, consecActividad);
		log.info("Calificación quitada: estudiante {}, matrícula {}", idEstudiante, idMatricula);
		return AcumuladoDto.desde(acumulado);
	}
}
