package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoNoEnviadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CredencialesInvalidasException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadosIntentosException;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleNoVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleYaVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.IdentidadExternaInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ServicioExternoNoDisponibleException;
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

	@ExceptionHandler(CorreoNoEnviadoException.class)
	ProblemDetail manejarCorreoNoEnviado(CorreoNoEnviadoException ex, HttpServletRequest peticion) {
		// El adaptador ya dejó en el log la causa técnica; aquí queda solo dónde pasó.
		log.warn("Código de verificación sin enviar en {}", peticion.getRequestURI());
		return problema(HttpStatus.SERVICE_UNAVAILABLE, "Correo no enviado", ex);
	}

	@ExceptionHandler(IdentidadExternaInvalidaException.class)
	ProblemDetail manejarIdentidadExternaInvalida(IdentidadExternaInvalidaException ex) {
		log.warn("Token de Google rechazado");
		return problema(HttpStatus.UNAUTHORIZED, "Cuenta de Google no válida", ex);
	}

	@ExceptionHandler(GoogleNoVinculadoException.class)
	ProblemDetail manejarGoogleNoVinculado(GoogleNoVinculadoException ex) {
		log.info("Inicio con Google rechazado: la cuenta de Google no está vinculada");
		return problema(HttpStatus.NOT_FOUND, "Google no vinculado", ex);
	}

	@ExceptionHandler(GoogleYaVinculadoException.class)
	ProblemDetail manejarGoogleYaVinculado(GoogleYaVinculadoException ex) {
		log.info("Vinculación con Google rechazada: {}", ex.getMessage());
		return problema(HttpStatus.CONFLICT, "Google ya vinculado", ex);
	}

	@ExceptionHandler(ServicioExternoNoDisponibleException.class)
	ProblemDetail manejarServicioExternoNoDisponible(ServicioExternoNoDisponibleException ex, HttpServletRequest peticion) {
		log.warn("Servicio externo no disponible en {}", peticion.getRequestURI());
		return problema(HttpStatus.SERVICE_UNAVAILABLE, "Servicio externo no disponible", ex);
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, RuntimeException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
		problema.setTitle(titulo);
		return problema;
	}
}
