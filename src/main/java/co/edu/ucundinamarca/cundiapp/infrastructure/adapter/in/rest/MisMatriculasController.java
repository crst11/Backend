package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.DefinirMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisAsignaturasMatriculadas;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AsignaturaMatriculadaDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.DefinirEstructuraDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.EstructuraDeEvaluacionDto;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Definir cómo me evalúan en cada asignatura (RF05, SCRUM-27).
 *
 * <p>La matrícula se busca siempre junto con el estudiante del token, así que pedir la de otro no
 * devuelve 403 sino "no encontramos esa asignatura": un 403 confirmaría que esa matrícula existe.
 */
@RestController
@RequestMapping("/api/mis/matriculas")
class MisMatriculasController {

	private static final Logger log = LoggerFactory.getLogger(MisMatriculasController.class);

	private final ListarMisAsignaturasMatriculadas listar;
	private final ConsultarMiEstructuraDeEvaluacion consultar;
	private final DefinirMiEstructuraDeEvaluacion definir;

	MisMatriculasController(
			ListarMisAsignaturasMatriculadas listar,
			ConsultarMiEstructuraDeEvaluacion consultar,
			DefinirMiEstructuraDeEvaluacion definir) {
		this.listar = listar;
		this.consultar = consultar;
		this.definir = definir;
	}

	@GetMapping
	List<AsignaturaMatriculadaDto> mias(@AuthenticationPrincipal Jwt token) {
		return listar.ejecutar(Integer.parseInt(token.getSubject())).stream()
				.map(AsignaturaMatriculadaDto::desde)
				.toList();
	}

	@GetMapping("/{idMatricula}/evaluacion")
	EstructuraDeEvaluacionDto evaluacion(@AuthenticationPrincipal Jwt token, @PathVariable int idMatricula) {
		return EstructuraDeEvaluacionDto.desde(
				consultar.ejecutar(Integer.parseInt(token.getSubject()), idMatricula));
	}

	@PutMapping("/{idMatricula}/evaluacion")
	EstructuraDeEvaluacionDto definir(
			@AuthenticationPrincipal Jwt token,
			@PathVariable int idMatricula,
			@Valid @RequestBody DefinirEstructuraDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var resultado = definir.ejecutar(idEstudiante, idMatricula, datos.aCategorias());
		log.info("Estructura de evaluación guardada: estudiante {}, matrícula {}, {} categorías y {} actividades",
				idEstudiante, idMatricula, resultado.categorias(), resultado.actividades());
		return EstructuraDeEvaluacionDto.desde(consultar.ejecutar(idEstudiante, idMatricula));
	}
}
