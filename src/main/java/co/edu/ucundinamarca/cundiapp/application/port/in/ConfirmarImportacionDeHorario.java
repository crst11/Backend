package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import java.util.List;

/**
 * Guarda el horario que el estudiante confirmó (RF04, SCRUM-81).
 *
 * <p>Lo que llega no se da por bueno: cada código se vuelve a cruzar contra la ruta de aprendizaje
 * y lo que no esté en el plan no se guarda. Para cada asignatura que ya tenía en el período, las
 * clases del reporte reemplazan a las que tenía: volver a importar actualiza, no duplica.
 */
public interface ConfirmarImportacionDeHorario {

	Resultado ejecutar(int idEstudiante, Confirmacion confirmacion);

	record Confirmacion(String periodo, List<AsignaturaConHorario> asignaturas) {

		public Confirmacion {
			if (periodo == null || periodo.isBlank()) {
				throw new ReglaDeNegocioVioladaException("Falta el período del horario que se importó");
			}
			if (asignaturas == null || asignaturas.isEmpty()) {
				throw new ReglaDeNegocioVioladaException("No hay nada que confirmar");
			}
			asignaturas = List.copyOf(asignaturas);
		}
	}

	/**
	 * Lo que de verdad quedó guardado: asignaturas nuevas en el período, asignaturas que ya estaban y
	 * se actualizaron, las omitidas (fuera del plan o que ya cerraron con nota) y las clases escritas.
	 */
	record Resultado(int nuevas, int actualizadas, int omitidas, int clases) {
	}
}
