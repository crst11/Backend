package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota;
import co.edu.ucundinamarca.cundiapp.domain.model.ProyeccionDeNota;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * La nota que hace falta para llegar a la meta (RF08, SCRUM-33 y SCRUM-34).
 *
 * <p>{@code notaRequerida} viene nula cuando la meta ya no se alcanza: la pantalla lo dice con
 * {@code situacion} ({@code alcanzada}, {@code alcanzable} o {@code inalcanzable}) en vez de
 * mostrar un número que pase de 5.0.
 */
public record ProyeccionDeNotaDto(
		AsignaturaMatriculadaDto asignatura,
		BigDecimal meta,
		BigDecimal puntos,
		BigDecimal porcentajeEvaluado,
		BigDecimal porcentajeRestante,
		BigDecimal notaRequerida,
		BigDecimal maximoPosible,
		String situacion) {

	public static ProyeccionDeNotaDto desde(SimularMiNota.Resultado resultado) {
		ProyeccionDeNota proyeccion = resultado.proyeccion();
		return new ProyeccionDeNotaDto(
				AsignaturaMatriculadaDto.desde(resultado.asignatura()),
				proyeccion.meta().valor(),
				proyeccion.puntos(),
				proyeccion.porcentajeEvaluado(),
				proyeccion.porcentajeRestante(),
				proyeccion.notaRequerida(),
				proyeccion.maximoPosible(),
				proyeccion.situacion().name().toLowerCase(Locale.ROOT));
	}
}
