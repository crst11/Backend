package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiPerfilAcademico;
import co.edu.ucundinamarca.cundiapp.application.port.in.ElegirMiPrograma;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ElegirProgramaDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.MiPerfilAcademicoDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El programa y el plan de estudios del estudiante (RF02, SCRUM-21). */
@RestController
@RequestMapping("/api/mis/perfil-academico")
class MiPerfilAcademicoController {

	private static final Logger log = LoggerFactory.getLogger(MiPerfilAcademicoController.class);

	private final ConsultarMiPerfilAcademico consultarPerfil;
	private final ElegirMiPrograma elegirPrograma;

	MiPerfilAcademicoController(ConsultarMiPerfilAcademico consultarPerfil, ElegirMiPrograma elegirPrograma) {
		this.consultarPerfil = consultarPerfil;
		this.elegirPrograma = elegirPrograma;
	}

	@GetMapping
	MiPerfilAcademicoDto perfil(@AuthenticationPrincipal Jwt token) {
		return MiPerfilAcademicoDto.desde(consultarPerfil.ejecutar(Integer.parseInt(token.getSubject())));
	}

	/** PUT y no POST: elegir programa se puede repetir para cambiarlo, y el resultado es el mismo. */
	@PutMapping
	MiPerfilAcademicoDto elegir(@AuthenticationPrincipal Jwt token, @Valid @RequestBody ElegirProgramaDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var perfil = elegirPrograma.ejecutar(idEstudiante, datos.codigoPrograma());
		log.info("Programa elegido: estudiante {} queda con el plan {}", idEstudiante,
				perfil.plan().map(plan -> plan.codigo()).orElse("ninguno"));
		return MiPerfilAcademicoDto.desde(perfil);
	}
}
