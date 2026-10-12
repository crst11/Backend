package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.AgregarClaseAlHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AgregarClaseDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.HorarioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.HorarioDto.ClaseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mi horario (RF04, SCRUM-25).
 *
 * <p>Como en el resto de {@code /api/mis}, el estudiante sale del token y nunca de un parámetro. Una
 * asignatura ajena responde "no encontramos esa asignatura", no 403: un 403 confirmaría que existe.
 */
@RestController
@RequestMapping("/api/mis")
class MiHorarioController {

	private static final Logger log = LoggerFactory.getLogger(MiHorarioController.class);

	private final ConsultarMiHorario consultar;
	private final AgregarClaseAlHorario agregar;
	private final QuitarClaseDelHorario quitar;

	MiHorarioController(ConsultarMiHorario consultar, AgregarClaseAlHorario agregar, QuitarClaseDelHorario quitar) {
		this.consultar = consultar;
		this.agregar = agregar;
		this.quitar = quitar;
	}

	@GetMapping("/horario")
	HorarioDto horario(@AuthenticationPrincipal Jwt token) {
		return HorarioDto.desde(consultar.ejecutar(Integer.parseInt(token.getSubject())));
	}

	@PostMapping("/matriculas/{idMatricula}/horario")
	@ResponseStatus(HttpStatus.CREATED)
	ClaseDto agregar(
			@AuthenticationPrincipal Jwt token,
			@PathVariable int idMatricula,
			@Valid @RequestBody AgregarClaseDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var clase = agregar.ejecutar(idEstudiante, idMatricula, datos.aDatos());
		// Sin el salón ni el docente en el log: solo qué pasó y de quién.
		log.info("Clase agregada al horario: estudiante {}, matrícula {}", idEstudiante, idMatricula);
		return ClaseDto.desde(clase);
	}

	@DeleteMapping("/matriculas/{idMatricula}/horario/{consecutivo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void quitar(@AuthenticationPrincipal Jwt token, @PathVariable int idMatricula, @PathVariable int consecutivo) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		quitar.ejecutar(idEstudiante, idMatricula, consecutivo);
		log.info("Clase quitada del horario: estudiante {}, matrícula {}", idEstudiante, idMatricula);
	}
}
