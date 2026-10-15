package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.AgregarClaseAlHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiPreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.GuardarMiPreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.FiltroDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.OrdenDelHorario;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AgregarClaseDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.HorarioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.HorarioDto.ClaseDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.PreferenciaDeHorarioDto;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mi horario (RF04, SCRUM-25 y SCRUM-26).
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
	private final ConsultarMiPreferenciaDeHorario consultarPreferencia;
	private final GuardarMiPreferenciaDeHorario guardarPreferencia;

	MiHorarioController(
			ConsultarMiHorario consultar,
			AgregarClaseAlHorario agregar,
			QuitarClaseDelHorario quitar,
			ConsultarMiPreferenciaDeHorario consultarPreferencia,
			GuardarMiPreferenciaDeHorario guardarPreferencia) {
		this.consultar = consultar;
		this.agregar = agregar;
		this.quitar = quitar;
		this.consultarPreferencia = consultarPreferencia;
		this.guardarPreferencia = guardarPreferencia;
	}

	/**
	 * Los criterios son opcionales y explícitos: el servidor no aplica por su cuenta la preferencia
	 * guardada. La pantalla la lee con {@code /preferencias-de-horario} y la manda aquí.
	 */
	@GetMapping("/horario")
	HorarioDto horario(
			@AuthenticationPrincipal Jwt token,
			@RequestParam(required = false) String asignatura,
			@RequestParam(required = false) String sede,
			@RequestParam(required = false, defaultValue = "hora") String orden) {
		return HorarioDto.desde(consultar.ejecutar(
				Integer.parseInt(token.getSubject()),
				new FiltroDeHorario(asignatura, sede),
				OrdenDelHorario.desdeBd(orden)));
	}

	@GetMapping("/preferencias-de-horario")
	PreferenciaDeHorarioDto preferencia(@AuthenticationPrincipal Jwt token) {
		return PreferenciaDeHorarioDto.desde(consultarPreferencia.ejecutar(Integer.parseInt(token.getSubject())));
	}

	@PutMapping("/preferencias-de-horario")
	PreferenciaDeHorarioDto guardarPreferencia(
			@AuthenticationPrincipal Jwt token, @Valid @RequestBody PreferenciaDeHorarioDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var guardada = guardarPreferencia.ejecutar(idEstudiante, datos.aPreferencia());
		// Sin la sede en el log: es lo mínimo que hace falta saber, y no hace falta saber dónde estudia.
		log.info("Preferencia de horario guardada: estudiante {}", idEstudiante);
		return PreferenciaDeHorarioDto.desde(guardada);
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
