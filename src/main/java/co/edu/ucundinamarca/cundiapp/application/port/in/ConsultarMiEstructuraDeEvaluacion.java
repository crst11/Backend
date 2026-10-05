package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;

/** Cómo se evalúa una asignatura matriculada (RF05, SCRUM-27). */
public interface ConsultarMiEstructuraDeEvaluacion {

	Resultado ejecutar(int idEstudiante, int idMatricula);

	/**
	 * Si la asignatura no está configurada, llega la propuesta de la plantilla sin guardar nada:
	 * el estudiante la ve, la ajusta y recién al confirmar se escribe. {@code guardada} dice cuál
	 * de las dos cosas está viendo, que es lo que la pantalla necesita para avisárselo.
	 */
	record Resultado(AsignaturaMatriculada asignatura, EstructuraDeEvaluacion estructura, boolean guardada) {
	}
}
