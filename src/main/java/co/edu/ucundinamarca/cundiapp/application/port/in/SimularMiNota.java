package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.ProyeccionDeNota;
import java.util.List;

/**
 * Qué nota hace falta en lo que queda, probando notas que todavía no existen (RF08, SCRUM-34).
 *
 * <p>**No escribe nada**: las notas hipotéticas viven solo en la petición. Las reales no se tocan y
 * una hipotética reemplaza a la real de la misma actividad solo para este cálculo.
 */
public interface SimularMiNota {

	/**
	 * @param meta la meta con la que calcular; {@code null} usa la que el estudiante tiene guardada,
	 *     sin cambiársela
	 * @param simuladas las notas hipotéticas; vacía devuelve la proyección con lo que ya hay
	 */
	Resultado ejecutar(int idEstudiante, int idMatricula, Meta meta, List<NotaSimulada> simuladas);

	record NotaSimulada(ClaveDeActividad actividad, Nota nota) {
	}

	record Resultado(AsignaturaMatriculada asignatura, ProyeccionDeNota proyeccion) {
	}
}
