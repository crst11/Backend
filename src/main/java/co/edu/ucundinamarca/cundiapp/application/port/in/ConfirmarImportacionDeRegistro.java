package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Guarda lo que el estudiante confirmó del reporte (RF03, SCRUM-23).
 *
 * <p>El estudiante puede haber corregido alguna nota antes de confirmar, así que lo que llega aquí
 * no se da por bueno: cada código se vuelve a cruzar contra la ruta de aprendizaje y cada nota se
 * valida contra la escala. Lo que no esté en el plan no se guarda.
 */
public interface ConfirmarImportacionDeRegistro {

	Resultado ejecutar(int idEstudiante, Confirmacion confirmacion);

	record Confirmacion(String nombreArchivo, int detectadas, List<PeriodoConfirmado> periodos) {

		public Confirmacion {
			if (nombreArchivo == null || nombreArchivo.isBlank()) {
				throw new ReglaDeNegocioVioladaException("Falta el nombre del archivo que se importó");
			}
			if (periodos == null || periodos.isEmpty()) {
				throw new ReglaDeNegocioVioladaException("No hay nada que confirmar");
			}
			periodos = List.copyOf(periodos);
		}
	}

	/**
	 * Un período con sus notas y con los totales que el reporte publicó para él. Los totales viajan
	 * aquí, y no aparte, porque son del período: el estudiante confirma el período completo tal como
	 * se lo mostramos.
	 */
	record PeriodoConfirmado(
			String codigo,
			int creditosMatriculados,
			int creditosAprobados,
			BigDecimal promedioPeriodo,
			BigDecimal promedioAcumulado,
			List<NotaConfirmada> notas) {

		public PeriodoConfirmado {
			notas = notas == null ? List.of() : List.copyOf(notas);
		}
	}

	record NotaConfirmada(String codigoAsignatura, BigDecimal nota) {
	}

	/** Lo que de verdad quedó guardado, para contárselo al estudiante sin inflar el número. */
	record Resultado(int idImportacion, int guardadas, int actualizadas, int omitidas) {
	}
}
