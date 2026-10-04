package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoPorImportar;
import java.util.List;

/**
 * Lee el reporte que subió el estudiante y le muestra lo detectado, sin guardar nada todavía
 * (RF03, SCRUM-23).
 *
 * <p>Nada toca su historial hasta que él confirme: ese es el criterio de aceptación, y también lo
 * razonable, porque una importación equivocada le reescribiría las notas.
 */
public interface AnalizarRegistroExtendido {

	Analisis ejecutar(int idEstudiante, byte[] archivo);

	/**
	 * Lo detectado, ya cruzado con la ruta de aprendizaje del estudiante.
	 *
	 * <p>`programaDelReporte` es el que dice el PDF; sirve para avisarle si subió el reporte de otro
	 * programa distinto al que eligió en la app.
	 */
	record Analisis(
			String programaDelReporte,
			String sedeDelReporte,
			boolean coincideConMiPrograma,
			List<PeriodoPorImportar> periodos) {

		public Analisis {
			periodos = periodos == null ? List.of() : List.copyOf(periodos);
		}

		public int detectadas() {
			return periodos.stream().mapToInt(periodo -> periodo.asignaturas().size()).sum();
		}

		public long seVanAGuardar() {
			return periodos.stream().mapToLong(PeriodoPorImportar::cuantasSeVanAGuardar).sum();
		}
	}
}
