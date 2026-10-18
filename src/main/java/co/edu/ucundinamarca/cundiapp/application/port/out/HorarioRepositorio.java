package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import java.util.List;
import java.util.Optional;

/** Lo que el núcleo necesita para guardar y leer el horario del estudiante (RF04, SCRUM-25). */
public interface HorarioRepositorio {

	/** Las clases de las asignaturas que el estudiante está cursando. Sin orden: lo pone el dominio. */
	List<ClaseDelHorario> clasesDe(int idEstudiante);

	/**
	 * La asignatura que el estudiante cursa, solo si es de él y está en curso. Vacío tanto si no existe
	 * como si es de otro: la respuesta no debe dejar averiguar qué matrículas hay.
	 */
	Optional<AsignaturaEnCurso> asignaturaEnCurso(int idEstudiante, int idMatricula);

	/** El número de orden del próximo bloque de la asignatura: uno más que el mayor que ya tiene. */
	int siguienteConsecutivo(int idMatricula);

	/**
	 * Guarda el bloque. El salón y el docente se reutilizan si ya existen, porque son los mismos de
	 * otras clases y de otros estudiantes: no se crea uno nuevo por cada bloque.
	 */
	void guardar(int idEstudiante, int idMatricula, BloqueDeHorario bloque);

	/** Falso si el bloque no existía, para que el caso de uso pueda decirlo. */
	boolean quitar(int idEstudiante, int idMatricula, int consecutivoDelBloque);

	record AsignaturaEnCurso(int idMatricula, String codigoAsignatura, String nombre) {
	}
}
