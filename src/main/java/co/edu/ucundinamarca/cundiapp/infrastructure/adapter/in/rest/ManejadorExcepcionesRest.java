package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoNoEnviadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CredencialesInvalidasException;
import co.edu.ucundinamarca.cundiapp.domain.exception.CuentaNoActivaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadasPeticionesException;
import co.edu.ucundinamarca.cundiapp.domain.exception.DemasiadosIntentosException;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleNoVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.GoogleYaVinculadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.IdentidadExternaInvalidaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ServicioExternoNoDisponibleException;
import co.edu.ucundinamarca.cundiapp.domain.exception.SesionInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio a respuestas RFC 9457 (ProblemDetail) y deja cada rechazo en
 * el log. Los mensajes del dominio no llevan datos personales, así que se pueden registrar tal cual.
 */
@RestControllerAdvice
class ManejadorExcepcionesRest {

	private static final Logger log = LoggerFactory.getLogger(ManejadorExcepcionesRest.class);

	/** El nombre de la restricción, que es lo único del mensaje de PostgreSQL que se puede registrar. */
	private static final Pattern RESTRICCION = Pattern.compile("constraint \"([^\"]+)\"");

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

	/** Un 429 con `Retry-After`: el navegador y quien integre saben cuánto esperar sin adivinar. */
	@ExceptionHandler(DemasiadasPeticionesException.class)
	ResponseEntity<ProblemDetail> manejarDemasiadasPeticiones(DemasiadasPeticionesException ex) {
		log.warn("Límite de peticiones alcanzado en una ruta que lo tiene");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, String.valueOf(DemasiadasPeticionesException.segundosDeEspera(ex.espera())))
				.body(problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas peticiones", ex));
	}

	@ExceptionHandler(CorreoNoEnviadoException.class)
	ProblemDetail manejarCorreoNoEnviado(CorreoNoEnviadoException ex, HttpServletRequest peticion) {
		// El adaptador ya dejó en el log la causa técnica; aquí queda solo dónde pasó.
		log.warn("Código de verificación sin enviar en {}", peticion.getRequestURI());
		return problema(HttpStatus.SERVICE_UNAVAILABLE, "Correo no enviado", ex);
	}

	/**
	 * Al entrar con Google, un token inválido es un 401 como cualquier credencial mala. En las rutas con
	 * sesión (vincular desde Mi cuenta) el 401 queda reservado a la sesión de CundiApp: si también
	 * significara "Google rechazó el token", el frontend creería que la sesión venció y la cerraría.
	 */
	@ExceptionHandler(IdentidadExternaInvalidaException.class)
	ProblemDetail manejarIdentidadExternaInvalida(IdentidadExternaInvalidaException ex, HttpServletRequest peticion) {
		log.warn("Token de Google rechazado en {}", peticion.getRequestURI());
		HttpStatus estado = peticion.getRequestURI().startsWith("/api/mis/")
				? HttpStatus.UNPROCESSABLE_ENTITY
				: HttpStatus.UNAUTHORIZED;
		return problema(estado, "Cuenta de Google no válida", ex);
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

	/**
	 * PostgreSQL rechazó una escritura: una restricción o un disparador que el dominio no previó. Sin este
	 * manejador la excepción sube sin tratar y el estudiante recibe un 403 con el mensaje del token CSRF
	 * (ver el comentario sobre /error en SeguridadConfig), que manda a depurar en la dirección equivocada.
	 *
	 * <p>El detalle técnico no viaja al cliente: el mensaje de PostgreSQL trae nombres de tablas,
	 * columnas y restricciones, y en las violaciones de CHECK también los valores de la fila. Por la misma
	 * razón al log solo va el código SQLSTATE y el nombre de la restricción, sin la fila.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail manejarViolacionDeIntegridad(DataIntegrityViolationException ex, HttpServletRequest peticion) {
		Throwable causa = NestedExceptionUtils.getMostSpecificCause(ex);
		Matcher nombre = RESTRICCION.matcher(String.valueOf(causa.getMessage()));
		String restriccion = nombre.find() ? nombre.group(1) : null;
		String estadoSql = causa instanceof SQLException sql ? sql.getSQLState() : null;
		log.warn("La base de datos rechazó una escritura en {} (SQLSTATE {}, restricción {})",
				peticion.getRequestURI(), estadoSql, restriccion);
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
				"No pudimos guardar: los datos no cumplen una regla del modelo");
		problema.setTitle("Violación de regla del modelo");
		return problema;
	}

	/**
	 * Lo que nadie previó: un error del servidor, que sale como 500 con un mensaje genérico (SCRUM-227).
	 *
	 * <p>Sin esto, la excepción salía del controlador, el contenedor reenviaba la petición a {@code /error}, Spring Security la bloqueaba
	 * ({@code denyAll}) y el cliente recibía un 403 con el mensaje del token CSRF: un error del servidor disfrazado de "no tienes
	 * permiso" (SCRUM-78). Ese disfraz escondía los fallos de quien vigila los 5xx y mandaba a depurar permisos que no estaban rotos.
	 *
	 * <p>Al cliente no viaja nada del error (ni el mensaje ni la clase): puede traer rutas, consultas o datos. Todo queda en el
	 * registro, con la traza, para quien lo investigue; la petición se reconoce por su {@code X-Request-Id}.
	 *
	 * <p>Los errores de MVC (cuerpo ilegible, método no permitido, validación...) no llegan aquí: los atiende antes el manejador de
	 * Spring Boot ({@code spring.mvc.problemdetails.enabled}). Los de seguridad se vuelven a lanzar: son de Spring Security, que los
	 * traduce a 401 y 403 y no deben convertirse en un 500.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail manejarLoInesperado(Exception ex, HttpServletRequest peticion) throws Exception {
		if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
			throw ex;
		}
		log.error("Error inesperado en {} {}", peticion.getMethod(), peticion.getRequestURI(), ex);
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Ocurrió un error inesperado. Intenta de nuevo.");
		problema.setTitle("Error del servidor");
		return problema;
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, RuntimeException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
		problema.setTitle(titulo);
		return problema;
	}
}
