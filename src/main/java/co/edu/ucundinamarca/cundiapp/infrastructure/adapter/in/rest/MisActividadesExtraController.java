package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnadirActividadExtra;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarActividadExtra;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AnadirActividadExtraDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.HorarioDto.ClaseDto;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mis actividades extra (SCRUM-266): lo de la universidad con día y hora que no es una asignatura. Se ven en Mi
 * horario y en Hoy por las rutas de siempre; aquí solo se añaden y se quitan.
 *
 * <p>El estudiante sale del token y nunca de un parámetro. Una actividad ajena responde «no encontramos esa
 * actividad», no 403: un 403 confirmaría que existe.
 */
@RestController
@RequestMapping("/api/mis/actividades-extra")
class MisActividadesExtraController {

	private static final Logger log = LoggerFactory.getLogger(MisActividadesExtraController.class);

	private final AnadirActividadExtra anadir;
	private final QuitarActividadExtra quitar;

	MisActividadesExtraController(AnadirActividadExtra anadir, QuitarActividadExtra quitar) {
		this.anadir = anadir;
		this.quitar = quitar;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	List<ClaseDto> anadir(@AuthenticationPrincipal Jwt token, @Valid @RequestBody AnadirActividadExtraDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var guardadas = anadir.ejecutar(idEstudiante, datos.aDatos());
		// Sin el nombre ni el lugar en el log: solo qué pasó y de quién.
		log.info("Actividad extra añadida: estudiante {}, {} día(s)", idEstudiante, guardadas.size());
		return guardadas.stream().map(ClaseDelHorario::deActividadExtra).map(ClaseDto::desde).toList();
	}

	@DeleteMapping("/{idActividad}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void quitar(@AuthenticationPrincipal Jwt token, @PathVariable int idActividad) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		quitar.ejecutar(idEstudiante, idActividad);
		log.info("Actividad extra quitada: estudiante {}", idEstudiante);
	}
}
