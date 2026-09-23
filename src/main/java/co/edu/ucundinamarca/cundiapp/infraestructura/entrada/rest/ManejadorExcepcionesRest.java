package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones del dominio a respuestas RFC 9457 (ProblemDetail). */
@RestControllerAdvice
class ManejadorExcepcionesRest {

	@ExceptionHandler(ReglaDeNegocioVioladaException.class)
	ProblemDetail manejarReglaDeNegocio(ReglaDeNegocioVioladaException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problema.setTitle("Violación de regla de negocio");
		return problema;
	}
}
