package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.PlantillaDeEvaluacion;
import java.util.List;
import java.util.Optional;

/** Lo que el núcleo necesita para guardar y leer el árbol de evaluación (RF05, SCRUM-27). */
public interface EstructuraDeEvaluacionRepositorio {

	/** Las matrículas del estudiante, de la más reciente a la más antigua. */
	List<AsignaturaMatriculada> matriculasDe(int idEstudiante);

	/**
	 * La matrícula, solo si es de este estudiante. Vacío tanto si no existe como si es de otro: la
	 * respuesta no debe dejar averiguar qué matrículas existen.
	 */
	Optional<AsignaturaMatriculada> matricula(int idEstudiante, int idMatricula);

	/** La estructura guardada. Vacía si el estudiante todavía no ha configurado la asignatura. */
	Optional<EstructuraDeEvaluacion> buscar(int idEstudiante, int idMatricula);

	/**
	 * Deja la estructura exactamente como viene: lo que ya no está se borra, y lo que sigue
	 * conserva su número de orden para no perder las notas que ya penden de él.
	 */
	void guardar(int idEstudiante, EstructuraDeEvaluacion estructura);

	/** Las notas registradas en la asignatura (SCRUM-28). Vacía si todavía no hay ninguna. */
	NotasDeLaAsignatura notasDe(int idEstudiante, int idMatricula);

	/**
	 * Guarda o reemplaza la nota de una actividad, y la marca como calificada. Es una sola
	 * transacción: la base de datos no deja calificar una actividad sin entregar, así que el estado
	 * se mueve antes de escribir la nota.
	 */
	void guardarNota(int idEstudiante, int idMatricula, ClaveDeActividad actividad, Nota nota);

	/**
	 * Quita la nota y devuelve la actividad a "no entregada". Falso si la actividad no tenía nota,
	 * para que el servicio pueda decirlo en vez de fingir que hizo algo.
	 */
	boolean quitarNota(int idEstudiante, int idMatricula, ClaveDeActividad actividad);

	/** La plantilla con la que arranca una asignatura sin configurar (la 30/30/40). */
	Optional<PlantillaDeEvaluacion> plantillaPredeterminada();
}
