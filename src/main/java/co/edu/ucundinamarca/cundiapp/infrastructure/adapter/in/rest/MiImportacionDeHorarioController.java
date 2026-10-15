package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarReporteDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AnalisisDeHorarioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ConfirmarHorarioDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ResultadoDeHorarioDto;
import jakarta.validation.Valid;
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
		byte[] contenido = ArchivoPdfSubido.leer(archivo, "Reporte de Horario");
		var analisis = analizar.ejecutar(idEstudiante, contenido);
		log.info("Reporte de horario analizado: estudiante {}, {} asignaturas y {} clases detectadas",
				idEstudiante, analisis.asignaturas().size(), analisis.clases());
		return AnalisisDeHorarioDto.desde(analisis, archivo.getOriginalFilename());
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
}
