package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarReporteDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AnalisisDeHorarioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ConfirmarHorarioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ResultadoDeHorarioDto;
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
 * Importar el Reporte de Horario (RF04, SCRUM-81): el mismo patrón de dos pasos del Registro
 * Académico Extendido. Primero se analiza y se le muestra al estudiante lo detectado, y solo cuando
 * él confirma se guarda.
 *
 * <p>Del reporte no se registra nada de identidad en el log: ni el nombre del estudiante ni los
 * documentos de los docentes, que el reporte trae pero la aplicación no lee.
 */
@RestController
@RequestMapping("/api/mis/importaciones/horario")
class MiImportacionDeHorarioController {

	private static final Logger log = LoggerFactory.getLogger(MiImportacionDeHorarioController.class);

	/** Un Reporte de Horario real pesa unas decenas de kB; más que esto no es ese reporte. */
	private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

	private final AnalizarReporteDeHorario analizar;
	private final ConfirmarImportacionDeHorario confirmar;

	MiImportacionDeHorarioController(AnalizarReporteDeHorario analizar, ConfirmarImportacionDeHorario confirmar) {
		this.analizar = analizar;
		this.confirmar = confirmar;
	}

	@PostMapping(path = "/analisis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	AnalisisDeHorarioDto analizar(
			@AuthenticationPrincipal Jwt token, @RequestPart("archivo") MultipartFile archivo) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		validar(archivo);
		try {
			var analisis = analizar.ejecutar(idEstudiante, archivo.getBytes());
			log.info("Reporte de horario analizado: estudiante {}, {} asignaturas y {} clases detectadas",
					idEstudiante, analisis.asignaturas().size(), analisis.clases());
			return AnalisisDeHorarioDto.desde(analisis, archivo.getOriginalFilename());
		} catch (IOException error) {
			throw new ReglaDeNegocioVioladaException("No pudimos leer el archivo que subiste");
		}
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResultadoDeHorarioDto confirmar(
			@AuthenticationPrincipal Jwt token, @Valid @RequestBody ConfirmarHorarioDto datos) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var resultado = confirmar.ejecutar(idEstudiante, datos.aConfirmacion());
		log.info("Horario importado: estudiante {}, {} asignaturas nuevas, {} actualizadas y {} clases",
				idEstudiante, resultado.nuevas(), resultado.actualizadas(), resultado.clases());
		return ResultadoDeHorarioDto.desde(resultado);
	}

	/** Un PDF es un archivo que viene de afuera: se revisa el tamaño y el tipo antes de abrirlo. */
	private static void validar(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Falta el archivo");
		}
		if (archivo.getSize() > TAMANO_MAXIMO) {
			throw new ReglaDeNegocioVioladaException(
					"El archivo pesa más de 5 MB; el Reporte de Horario pesa mucho menos. Revisa que sea el reporte correcto");
		}
		String nombre = archivo.getOriginalFilename();
		if (nombre == null || !nombre.toLowerCase().endsWith(".pdf")) {
			throw new ReglaDeNegocioVioladaException("El reporte tiene que ser el archivo PDF que descargaste");
		}
	}
}
