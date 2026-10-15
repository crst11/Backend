package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import java.util.List;

/**
 * Lee el Reporte de Horario que subió el estudiante y le muestra lo detectado, sin guardar nada
 * todavía (RF04, SCRUM-81). Es el primer paso del mismo patrón de dos pasos del Registro Académico
 * Extendido: nada toca su horario hasta que él confirme.
 */
public interface AnalizarReporteDeHorario {

	Analisis ejecutar(int idEstudiante, byte[] archivo);

	/**
	 * Lo detectado, ya cruzado con la ruta de aprendizaje del estudiante. `periodo` se deduce de la
	 * fecha en que se imprimió el reporte, que no dice a qué período corresponde.
	 */
	record Analisis(
			String programaDelReporte,
			String sedeDelReporte,
			boolean coincideConMiPrograma,
			String periodo,
			List<AsignaturaPorImportar> asignaturas) {

		public Analisis {
			asignaturas = asignaturas == null ? List.of() : List.copyOf(asignaturas);
		}

		public int clases() {
			return asignaturas.stream().mapToInt(asignatura -> asignatura.bloques().size()).sum();
		}

		public long seVanAGuardar() {
			return asignaturas.stream().filter(AsignaturaPorImportar::enElPlan).count();
		}
	}

	/**
	 * `yaEstaba` dice si el estudiante ya tiene esa asignatura en este período, y `clasesQueTiene`
	 * cuántas clases le tiene guardadas hoy: son las que el reporte reemplaza al confirmar.
	 */
	record AsignaturaPorImportar(
			String codigo,
			String nombre,
			String grupo,
			boolean enElPlan,
			boolean yaEstaba,
			int clasesQueTiene,
			List<BloqueDeHorario> bloques) {

		public AsignaturaPorImportar {
			bloques = bloques == null ? List.of() : List.copyOf(bloques);
		}
	}
}
