package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.in.MarcarMiAvisoComoLeido;
import co.edu.ucundinamarca.cundiapp.application.port.in.MarcarMisAvisosComoLeidos;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AvisosDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.MarcarAvisoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Mis avisos (RF10, SCRUM-37): la campana de la aplicación. Todo aviso se ve aquí, haya llegado o no al
 * dispositivo por Web Push. Como en el resto de {@code /api/mis}, el estudiante sale del token y un aviso
 * ajeno responde «no encontramos ese aviso».
 */
@RestController
@RequestMapping("/api/mis/avisos")
class MisAvisosController {

	private final ListarMisAvisos listar;
	private final MarcarMiAvisoComoLeido marcar;
	private final MarcarMisAvisosComoLeidos marcarTodos;

	MisAvisosController(ListarMisAvisos listar, MarcarMiAvisoComoLeido marcar, MarcarMisAvisosComoLeidos marcarTodos) {
		this.listar = listar;
		this.marcar = marcar;
		this.marcarTodos = marcarTodos;
	}

	@GetMapping
	AvisosDto listar(@AuthenticationPrincipal Jwt token, @RequestParam(defaultValue = "false") boolean soloSinLeer) {
		return AvisosDto.desde(listar.ejecutar(Integer.parseInt(token.getSubject()), soloSinLeer));
	}

	@PatchMapping("/{idAviso}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void marcar(@AuthenticationPrincipal Jwt token, @PathVariable int idAviso, @Valid @RequestBody MarcarAvisoDto datos) {
		marcar.ejecutar(Integer.parseInt(token.getSubject()), idAviso, datos.leido());
	}

	@PostMapping("/leidos")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void marcarTodos(@AuthenticationPrincipal Jwt token) {
		marcarTodos.ejecutar(Integer.parseInt(token.getSubject()));
	}
}
