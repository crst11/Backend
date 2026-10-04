package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import java.util.List;

/**
 * Guardar cómo evalúan una asignatura (RF05, SCRUM-27).
 *
 * <p>Llega el árbol completo, no un cambio suelto: el estudiante mueve varios pesos a la vez y
 * entre un paso y otro la suma no da 100 %. Guardar actividad por actividad obligaría a rechazar
 * estados intermedios que son parte normal de editar.
 */
public interface DefinirMiEstructuraDeEvaluacion {

	Resultado ejecutar(int idEstudiante, int idMatricula, List<CategoriaDeEvaluacion> categorias);

	record Resultado(int idMatricula, int categorias, int actividades) {
	}
}
