package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarRegistroExtendido;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro;
import co.edu.ucundinamarca.cundiapp.application.port.in.DeshacerImportacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisImportaciones;
import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.AnalisisDeImportacionDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ConfirmarImportacionDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ImportacionDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ResultadoDeDeshacerDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ResultadoDeImportacionDto;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final ListarMisImportaciones listarImportaciones;
	private final DeshacerImportacion deshacer;

	MisImportacionesController(
			AnalizarRegistroExtendido analizar,
			ConfirmarImportacionDeRegistro confirmar,
			ListarMisImportaciones listarImportaciones,
			DeshacerImportacion deshacer) {
		this.analizar = analizar;
		this.confirmar = confirmar;
		this.listarImportaciones = listarImportaciones;
		this.deshacer = deshacer;
	}

	/**
	 * El historial de cargas, de la más reciente a la más antigua (SCRUM-24). Solo la última
	 * confirmada viene marcada como deshacible: esa regla la decide el servidor, no la pantalla.
	 */
	@GetMapping
	List<ImportacionDto> mias(@AuthenticationPrincipal Jwt token) {
		List<Importacion> cargas = listarImportaciones.ejecutar(Integer.parseInt(token.getSubject()));
		Integer ultimaConfirmada = cargas.stream()
				.filter(Importacion::sePuedeDeshacer)
				.map(Importacion::id)
				.findFirst()
				.orElse(null);
		return cargas.stream()
				.map(carga -> ImportacionDto.desde(carga, Integer.valueOf(carga.id()).equals(ultimaConfirmada)))
				.toList();
	}

	@PostMapping("/{idImportacion}/reversion")
	ResultadoDeDeshacerDto deshacer(
			@AuthenticationPrincipal Jwt token, @PathVariable int idImportacion) {
		int idEstudiante = Integer.parseInt(token.getSubject());
		var resultado = deshacer.ejecutar(idEstudiante, idImportacion);
		log.info("Importación {} deshecha: estudiante {}, {} eliminadas y {} restauradas",
				idImportacion, idEstudiante, resultado.eliminadas(), resultado.restauradas());
		return ResultadoDeDeshacerDto.desde(resultado);
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
