package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarCalendario;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CalendarioDto;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Qué clase de día es cada fecha: lectivo, festivo o fuera del período (RF07, SCRUM-85). No es de
 * nadie en particular, pero tampoco es público.
 */
@RestController
@RequestMapping("/api/calendario")
class CalendarioController {

	private final ConsultarCalendario consultar;

	CalendarioController(ConsultarCalendario consultar) {
		this.consultar = consultar;
	}

	@GetMapping("/dias")
	CalendarioDto dias(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return CalendarioDto.desde(consultar.ejecutar(desde, hasta));
	}
}
