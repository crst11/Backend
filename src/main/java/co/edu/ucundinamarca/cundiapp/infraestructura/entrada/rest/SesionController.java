package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.CerrarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.IniciarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.OrigenDeSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RenovarSesion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.SesionIniciada;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.SesionInvalidaException;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.LoginDto;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.SesionDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio, renovación y cierre de sesión (RF01). El token de refresco vive en una cookie HttpOnly
 * limitada a esta ruta; con ella viaja una cookie XSRF-TOKEN legible por el frontend, que debe
 * devolverla en el encabezado X-XSRF-TOKEN al renovar o cerrar sesión (protección CSRF).
 */
@RestController
@RequestMapping("/api/publico/auth")
class SesionController {

	static final String COOKIE_REFRESCO = "refresco";
	static final String COOKIE_CSRF = "XSRF-TOKEN";
	private static final String RUTA_COOKIE_REFRESCO = "/api/publico/auth";

	private final IniciarSesion iniciarSesion;
	private final RenovarSesion renovarSesion;
	private final CerrarSesion cerrarSesion;
	private final boolean cookieSegura;
	private final SecureRandom azar = new SecureRandom();

	SesionController(
			IniciarSesion iniciarSesion,
			RenovarSesion renovarSesion,
			CerrarSesion cerrarSesion,
			@Value("${cundiapp.refresco.cookie-secure}") boolean cookieSegura) {
		this.iniciarSesion = iniciarSesion;
		this.renovarSesion = renovarSesion;
		this.cerrarSesion = cerrarSesion;
		this.cookieSegura = cookieSegura;
	}

	@PostMapping("/login")
	ResponseEntity<SesionDto> login(@Valid @RequestBody LoginDto datos, HttpServletRequest peticion) {
		return responder(iniciarSesion.ejecutar(datos.correo(), datos.contrasena(), origenDe(peticion)));
	}

	@PostMapping("/refresco")
	ResponseEntity<SesionDto> refrescar(
			@CookieValue(name = COOKIE_REFRESCO, required = false) String tokenDeRefresco, HttpServletRequest peticion) {
		if (tokenDeRefresco == null || tokenDeRefresco.isBlank()) {
			throw new SesionInvalidaException();
		}
		return responder(renovarSesion.ejecutar(tokenDeRefresco, origenDe(peticion)));
	}

	@PostMapping("/logout")
	ResponseEntity<Void> cerrar(@CookieValue(name = COOKIE_REFRESCO, required = false) String tokenDeRefresco) {
		if (tokenDeRefresco != null && !tokenDeRefresco.isBlank()) {
			cerrarSesion.ejecutar(tokenDeRefresco);
		}
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookie(COOKIE_REFRESCO, "", RUTA_COOKIE_REFRESCO, true, Duration.ZERO).toString())
				.header(HttpHeaders.SET_COOKIE, cookie(COOKIE_CSRF, "", "/", false, Duration.ZERO).toString())
				.build();
	}

	private ResponseEntity<SesionDto> responder(SesionIniciada sesion) {
		Instant ahora = Instant.now();
		Duration vigencia = Duration.between(ahora, sesion.refrescoExpira());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE,
						cookie(COOKIE_REFRESCO, sesion.tokenDeRefresco(), RUTA_COOKIE_REFRESCO, true, vigencia).toString())
				.header(HttpHeaders.SET_COOKIE, cookie(COOKIE_CSRF, nuevoTokenCsrf(), "/", false, vigencia).toString())
				.body(SesionDto.desde(sesion, ahora));
	}

	private ResponseCookie cookie(String nombre, String valor, String ruta, boolean soloHttp, Duration vigencia) {
		return ResponseCookie.from(nombre, valor)
				.httpOnly(soloHttp)
				.secure(cookieSegura)
				.sameSite("Strict")
				.path(ruta)
				.maxAge(vigencia)
				.build();
	}

	private String nuevoTokenCsrf() {
		byte[] bytes = new byte[24];
		azar.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static OrigenDeSesion origenDe(HttpServletRequest peticion) {
		return new OrigenDeSesion(peticion.getRemoteAddr(), peticion.getHeader(HttpHeaders.USER_AGENT));
	}
}
