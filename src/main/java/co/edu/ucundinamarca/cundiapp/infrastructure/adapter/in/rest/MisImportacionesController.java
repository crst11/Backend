package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarRegistroExtendido;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AnalisisDeImportacionDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ConfirmarImportacionDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ResultadoDeImportacionDto;
import jakarta.validation.Valid;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importar el Registro Académico Extendido (RF03, SCRUM-23).
 *
 * <p>Son dos pasos a propósito: primero se analiza y se le muestra al estudiante lo detectado, y
 * solo cuando él confirma se guarda. Una importación que escribiera de una sola vez le reescribiría
 * las notas sin que alcance a mirarlas.
 */
@RestController
@RequestMapping("/api/mis/importaciones")
class MisImportacionesController {

	private static final Logger log = LoggerFactory.getLogger(MisImportacionesController.class);

	/** Un registro extendido real pesa unos pocos cientos de kB; más que esto no es ese reporte. */
	private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

	private final AnalizarRegistroExtendido analizar;
	private final ConfirmarImportacionDeRegistro confirmar;

	MisImportacionesController(
			AnalizarRegistroExtendido analizar, ConfirmarImportacionDeRegistro confirmar) {
		this.analizar = analizar;
		this.confirmar = confirmar;
	}

	@PostMapping(path = "/analisis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	AnalisisDeImportacionDto analizar(
			@AuthenticationPrincipal Jwt token, @RequestPart("archivo") MultipartFile archivo) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		validar(archivo);
		try {
			var analisis = analizar.ejecutar(idEstudiante, archivo.getBytes());
			log.info("Reporte analizado: estudiante {}, {} asignaturas detectadas",
					idEstudiante, analisis.detectadas());
			return AnalisisDeImportacionDto.desde(analisis, archivo.getOriginalFilename());
		} catch (IOException error) {
			throw new ReglaDeNegocioVioladaException("No pudimos leer el archivo que subiste");
		}
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResultadoDeImportacionDto confirmar(
			@AuthenticationPrincipal Jwt token, @Valid @RequestBody ConfirmarImportacionDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var resultado = confirmar.ejecutar(idEstudiante, datos.aConfirmacion());
		log.info("Importación {} confirmada: estudiante {}, {} nuevas y {} actualizadas",
				resultado.idImportacion(), idEstudiante, resultado.guardadas(), resultado.actualizadas());
		return ResultadoDeImportacionDto.desde(resultado);
	}

	/**
	 * Un PDF es un archivo que viene de afuera. Se revisa el tamaño y el tipo antes de abrirlo:
	 * dejar que la librería se tope con cualquier cosa es cómo se gastan los recursos del servidor.
	 */
	private static void validar(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Falta el archivo");
		}
		if (archivo.getSize() > TAMANO_MAXIMO) {
			throw new ReglaDeNegocioVioladaException(
					"El archivo pesa más de 5 MB; el Registro Académico Extendido pesa mucho menos. Revisa que sea el reporte correcto");
		}
		String nombre = archivo.getOriginalFilename();
		if (nombre == null || !nombre.toLowerCase().endsWith(".pdf")) {
			throw new ReglaDeNegocioVioladaException("El reporte tiene que ser el archivo PDF que descargaste");
		}
	}
}
