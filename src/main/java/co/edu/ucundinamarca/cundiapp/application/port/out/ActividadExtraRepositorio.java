package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.ActividadExtra;
import java.util.List;
import java.util.Optional;

/** Lo que el núcleo necesita para guardar y leer las actividades extra del estudiante (SCRUM-266). */
public interface ActividadExtraRepositorio {

	/**
	 * Guarda todas las actividades o ninguna: vacío, sin guardar nada, si con ellas el estudiante pasaría de
	 * {@code maximo}. La cuenta y el guardado van juntos y en orden por estudiante, porque dos peticiones a la vez
	 * contando por separado dejarían pasar a las dos.
	 */
	Optional<List<ActividadExtra>> guardarSiCaben(int idEstudiante, List<ActividadExtra> actividades, int maximo);

	/** Las actividades del estudiante, sin orden: lo pone el dominio. */
	List<ActividadExtra> de(int idEstudiante);

	/** Falso si no existía o es de otro estudiante: la respuesta no debe dejar averiguar cuál de las dos. */
	boolean quitar(int idEstudiante, int idActividad);
}
