package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CredencialesInvalidasException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadosIntentosException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio a respuestas RFC 9457 (ProblemDetail) y deja cada rechazo en
 * el log. Los mensajes del dominio no llevan datos personales, así que se pueden registrar tal cual.
 */
@RestControllerAdvice
class ManejadorExcepcionesRest {

	private static final Logger log = LoggerFactory.getLogger(ManejadorExcepcionesRest.class);

	@ExceptionHandler(ReglaDeNegocioVioladaException.class)
	ProblemDetail manejarReglaDeNegocio(ReglaDeNegocioVioladaException ex, HttpServletRequest peticion) {
		log.info("Regla de negocio violada en {}: {}", peticion.getRequestURI(), ex.getMessage());
		return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Violación de regla de negocio", ex);
	}

	@ExceptionHandler(CorreoYaRegistradoException.class)
	ProblemDetail manejarCorreoDuplicado(CorreoYaRegistradoException ex) {
		log.info("Registro rechazado: el correo ya tiene una cuenta");
		return problema(HttpStatus.CONFLICT, "Correo ya registrado", ex);
	}

	@ExceptionHandler(CredencialesInvalidasException.class)
	ProblemDetail manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
		log.warn("Inicio de sesión fallido: credenciales incorrectas");
		return problema(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", ex);
	}

	@ExceptionHandler(SesionInvalidaException.class)
	ProblemDetail manejarSesionInvalida(SesionInvalidaException ex, HttpServletRequest peticion) {
		log.warn("Sesión inválida o expirada en {}", peticion.getRequestURI());
		return problema(HttpStatus.UNAUTHORIZED, "Sesión inválida", ex);
	}

	@ExceptionHandler(CuentaNoActivaException.class)
	ProblemDetail manejarCuentaNoActiva(CuentaNoActivaException ex) {
		log.info("Inicio de sesión rechazado: la cuenta no está activa");
		return problema(HttpStatus.FORBIDDEN, "Cuenta no activa", ex);
	}

	@ExceptionHandler(DemasiadosIntentosException.class)
	ProblemDetail manejarDemasiadosIntentos(DemasiadosIntentosException ex) {
		log.warn("Inicio de sesión bloqueado por demasiados intentos fallidos seguidos");
		return problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", ex);
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, RuntimeException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
		problema.setTitle(titulo);
		return problema;
	}
}
