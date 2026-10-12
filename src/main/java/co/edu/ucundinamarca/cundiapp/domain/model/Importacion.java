package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.Instant;

/**
 * Una carga de reporte que el estudiante hizo, como la ve en su historial de importaciones
 * (RF03, SCRUM-24).
 */
public record Importacion(
		int id,
		String tipoReporte,
		String nombreArchivo,
		Instant fechaCarga,
		EstadoDeImportacion estado,
		int detectadas,
		int confirmadas) {

	public boolean sePuedeDeshacer() {
		return estado.sePuedeDeshacer();
	}
}
