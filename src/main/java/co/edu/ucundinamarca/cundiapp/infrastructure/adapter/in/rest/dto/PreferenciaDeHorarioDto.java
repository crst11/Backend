package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.OrdenDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.VistaDelHorario;
import jakarta.validation.constraints.NotBlank;

/**
 * Cómo prefiere el estudiante ver su horario, en las dos direcciones (SCRUM-26): lo que se lee y lo
 * que se guarda. La vista y el orden son obligatorios; la sede nula es «todas».
 *
 * <p>Que la vista y el orden existan, y que la sede quepa, lo comprueba el dominio, para que el mensaje
 * al estudiante sea el mismo venga de donde venga.
 */
public record PreferenciaDeHorarioDto(@NotBlank String vista, String sede, @NotBlank String orden) {

	public PreferenciaDeHorario aPreferencia() {
		return new PreferenciaDeHorario(VistaDelHorario.desdeBd(vista), sede, OrdenDelHorario.desdeBd(orden));
	}

	public static PreferenciaDeHorarioDto desde(PreferenciaDeHorario preferencia) {
		return new PreferenciaDeHorarioDto(
				preferencia.vista().valorEnBd(), preferencia.sede(), preferencia.orden().valorEnBd());
	}
}
