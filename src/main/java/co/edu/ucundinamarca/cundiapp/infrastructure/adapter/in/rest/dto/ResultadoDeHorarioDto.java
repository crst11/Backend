package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario.Resultado;

/** Lo que de verdad quedó guardado del horario importado, para contárselo sin inflar el número. */
public record ResultadoDeHorarioDto(int nuevas, int actualizadas, int omitidas, int clases) {

	public static ResultadoDeHorarioDto desde(Resultado resultado) {
		return new ResultadoDeHorarioDto(
				resultado.nuevas(), resultado.actualizadas(), resultado.omitidas(), resultado.clases());
	}
}
