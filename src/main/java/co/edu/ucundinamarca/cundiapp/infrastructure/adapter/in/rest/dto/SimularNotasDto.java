package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota.NotaSimulada;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Las notas hipotéticas que manda el simulador, y opcionalmente otra meta (SCRUM-34). Nada de esto
 * se guarda.
 *
 * <p>Solo se exige que lo obligatorio venga; que la meta y las notas estén entre 0.0 y 5.0 con dos
 * decimales lo comprueba el dominio, para que el mensaje sea el mismo venga de donde venga.
 */
public record SimularNotasDto(BigDecimal meta, @Size(max = 200) List<@Valid NotaSimuladaDto> notas) {

	public record NotaSimuladaDto(
			@NotNull Integer consecCategoria, @NotNull Integer consecActividad, @NotNull BigDecimal nota) {
	}

	public Meta aMeta() {
		return meta == null ? null : new Meta(meta);
	}

	public List<NotaSimulada> aNotasSimuladas() {
		if (notas == null) {
			return List.of();
		}
		return notas.stream()
				.map(n -> new NotaSimulada(new ClaveDeActividad(n.consecCategoria(), n.consecActividad()), new Nota(n.nota())))
				.toList();
	}
}
