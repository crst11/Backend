package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;
import java.time.Instant;

/**
 * Una carga del historial de importaciones (SCRUM-24).
 *
 * <p>`sePuedeDeshacer` lo decide el servidor y no la pantalla: solo la última carga confirmada se
 * puede deshacer, porque deshacer una anterior dejaría encima los cambios de las que vinieron
 * después.
 */
public record ImportacionDto(
		int id,
		String tipoReporte,
		String nombreArchivo,
		Instant fechaCarga,
		String estado,
		int detectadas,
		int confirmadas,
		boolean sePuedeDeshacer) {

	public static ImportacionDto desde(Importacion importacion, boolean esLaUltimaConfirmada) {
		return new ImportacionDto(
				importacion.id(),
				importacion.tipoReporte(),
				importacion.nombreArchivo(),
				importacion.fechaCarga(),
				importacion.estado().valorEnBd(),
				importacion.detectadas(),
				importacion.confirmadas(),
				importacion.sePuedeDeshacer() && esLaUltimaConfirmada);
	}
}
