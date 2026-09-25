package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CredencialesInvalidasException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadosIntentosException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones del dominio a respuestas RFC 9457 (ProblemDetail). */
@RestControllerAdvice
class ManejadorExcepcionesRest {

	@ExceptionHandler(ReglaDeNegocioVioladaException.class)
	ProblemDetail manejarReglaDeNegocio(ReglaDeNegocioVioladaException ex) {
		return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Violación de regla de negocio", ex);
	}

	@ExceptionHandler(CorreoYaRegistradoException.class)
	ProblemDetail manejarCorreoDuplicado(CorreoYaRegistradoException ex) {
		return problema(HttpStatus.CONFLICT, "Correo ya registrado", ex);
	}

	@ExceptionHandler(CredencialesInvalidasException.class)
	ProblemDetail manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", ex);
	}

	@ExceptionHandler(SesionInvalidaException.class)
	ProblemDetail manejarSesionInvalida(SesionInvalidaException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "Sesión inválida", ex);
	}

	@ExceptionHandler(CuentaNoActivaException.class)
	ProblemDetail manejarCuentaNoActiva(CuentaNoActivaException ex) {
		return problema(HttpStatus.FORBIDDEN, "Cuenta no activa", ex);
	}

	@ExceptionHandler(DemasiadosIntentosException.class)
	ProblemDetail manejarDemasiadosIntentos(DemasiadosIntentosException ex) {
		return problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", ex);
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, RuntimeException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
		problema.setTitle(titulo);
		return problema;
	}
}
