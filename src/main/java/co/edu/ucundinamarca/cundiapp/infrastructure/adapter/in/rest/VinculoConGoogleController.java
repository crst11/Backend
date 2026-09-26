package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarVinculoConGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.in.DesvincularGoogle;
import co.edu.ucundinamarca.cundiapp.application.port.in.VincularGoogle;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.GoogleDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.VinculoConGoogleDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vincular, consultar y quitar el inicio con Google de la cuenta autenticada (SCRUM-48). */
@RestController
@RequestMapping("/api/mis/google")
class VinculoConGoogleController {

	private static final Logger log = LoggerFactory.getLogger(VinculoConGoogleController.class);

	private final ConsultarVinculoConGoogle consultar;
	private final VincularGoogle vincular;
	private final DesvincularGoogle desvincular;

	VinculoConGoogleController(ConsultarVinculoConGoogle consultar, VincularGoogle vincular, DesvincularGoogle desvincular) {
		this.consultar = consultar;
		this.vincular = vincular;
		this.desvincular = desvincular;
	}

	@GetMapping
	VinculoConGoogleDto consultar(@AuthenticationPrincipal Jwt token) {
		return VinculoConGoogleDto.desde(consultar.ejecutar(estudianteDe(token)));
	}

	@PostMapping
	VinculoConGoogleDto vincular(@AuthenticationPrincipal Jwt token, @Valid @RequestBody GoogleDto datos) {
		int idEstudiante = estudianteDe(token);
		var vinculo = vincular.ejecutar(idEstudiante, datos.idToken());
		log.info("Google vinculado: estudiante {}", idEstudiante);
		return VinculoConGoogleDto.desde(vinculo);
	}

	@DeleteMapping
	ResponseEntity<Void> desvincular(@AuthenticationPrincipal Jwt token) {
		int idEstudiante = estudianteDe(token);
		desvincular.ejecutar(idEstudiante);
		log.info("Google desvinculado: estudiante {}", idEstudiante);
		return ResponseEntity.noContent().build();
	}

	private static int estudianteDe(Jwt token) {
		return Integer.parseInt(token.getSubject());
	}
}
