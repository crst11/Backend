package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.Resultado;

/**
 * Lo que de verdad quedó guardado (SCRUM-23). `omitidas` son las que el reporte traía pero no
 * están en la ruta de aprendizaje: se cuentan para poder decírselo al estudiante en vez de callarlo.
 */
public record ResultadoDeImportacionDto(
		int idImportacion, int guardadas, int actualizadas, int omitidas) {

	public static ResultadoDeImportacionDto desde(Resultado resultado) {
		return new ResultadoDeImportacionDto(
				resultado.idImportacion(), resultado.guardadas(),
				resultado.actualizadas(), resultado.omitidas());
	}
}
