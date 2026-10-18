package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarLlaveDePush;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisDispositivosConAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarMiDispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiSuscripcionPush;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.DispositivoConAvisosDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.LlaveDePushDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.SuscripcionPushDto;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los dispositivos que reciben avisos (RF10, SCRUM-37): la llave con la que el navegador se suscribe y el
 * registro de su suscripción. El estudiante sale del token.
 */
@RestController
@RequestMapping("/api/mis/dispositivos")
class MisDispositivosController {

	private static final Logger log = LoggerFactory.getLogger(MisDispositivosController.class);

	private final ConsultarLlaveDePush llave;
	private final RegistrarMiSuscripcionPush registrar;
	private final ListarMisDispositivosConAvisos listar;
	private final QuitarMiDispositivoConAvisos quitar;

	MisDispositivosController(
			ConsultarLlaveDePush llave,
			RegistrarMiSuscripcionPush registrar,
			ListarMisDispositivosConAvisos listar,
			QuitarMiDispositivoConAvisos quitar) {
		this.llave = llave;
		this.registrar = registrar;
		this.listar = listar;
		this.quitar = quitar;
	}

	/** Los dispositivos que hoy reciben mis avisos (SCRUM-38). */
	@GetMapping
	List<DispositivoConAvisosDto> listar(@AuthenticationPrincipal Jwt token) {
		return listar.ejecutar(Integer.parseInt(token.getSubject())).stream().map(DispositivoConAvisosDto::desde).toList();
	}

	/** Deja de mandar avisos a uno de mis dispositivos; uno ajeno responde «no encontramos ese dispositivo». */
	@DeleteMapping("/{consecutivo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void quitar(@AuthenticationPrincipal Jwt token, @PathVariable int consecutivo) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		quitar.ejecutar(idEstudiante, consecutivo);
		log.info("Dispositivo quitado de los avisos: estudiante {}", idEstudiante);
	}

	@GetMapping("/llave-publica")
	LlaveDePushDto llavePublica() {
		return LlaveDePushDto.desde(llave.ejecutar());
	}

	/**
	 * Idempotente: el mismo navegador que vuelve a suscribirse actualiza su fila. El nombre del dispositivo sale
	 * del user agent; la dirección y las llaves no se registran en el log, porque son secretas.
	 */
	@PutMapping("/suscripcion")
	DispositivoConAvisosDto suscribir(
			@AuthenticationPrincipal Jwt token,
			@RequestHeader(value = "User-Agent", required = false) String userAgent,
			@Valid @RequestBody SuscripcionPushDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var dispositivo = registrar.ejecutar(idEstudiante, NombreDeDispositivo.desde(userAgent), datos.aSuscripcion());
		log.info("Avisos activados en un dispositivo: estudiante {}", idEstudiante);
		return DispositivoConAvisosDto.desde(dispositivo);
	}
}
