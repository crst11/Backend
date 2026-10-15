package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;

/** Cómo se evalúa una asignatura matriculada y cómo va en ella (RF05, SCRUM-27 y SCRUM-28). */
public interface ConsultarMiEstructuraDeEvaluacion {

	Resultado ejecutar(int idEstudiante, int idMatricula);

	/**
	 * Si la asignatura no está configurada, llega la propuesta de la plantilla sin guardar nada:
	 * el estudiante la ve, la ajusta y recién al confirmar se escribe. {@code guardada} dice cuál
	 * de las dos cosas está viendo, que es lo que la pantalla necesita para avisárselo.
	 *
	 * <p>Las notas y el acumulado van junto con la estructura porque la pantalla de notas necesita
	 * las dos cosas a la vez, y pedirlas por separado dejaría ver una mitad desactualizada.
	 */
	record Resultado(
			AsignaturaMatriculada asignatura,
			EstructuraDeEvaluacion estructura,
			boolean guardada,
			NotasDeLaAsignatura notas,
			NotaAcumulada acumulado) {
	}
}
