package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.RecursoInstitucional;
import java.util.List;

/** Lo que el núcleo necesita para leer los recursos de la guía institucional. */
public interface RecursoInstitucionalRepositorio {

	/**
	 * Los recursos que se pueden mostrar sin cuenta (no retirados y sin autenticación de por medio),
	 * en el orden de sus categorías y, dentro de cada una, con lo más consultado primero.
	 */
	List<RecursoInstitucional> listarPublicados();
}
