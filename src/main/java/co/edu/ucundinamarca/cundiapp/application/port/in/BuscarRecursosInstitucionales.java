package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.RecursoInstitucional;
import java.util.List;

/** Caso de uso: buscar documentos oficiales de la guía, sin cuenta (RF11, SCRUM-19). */
public interface BuscarRecursosInstitucionales {

	/**
	 * @param busqueda texto libre; vacío o nulo devuelve todos
	 * @param idCategoria solo los de esa categoría; nulo para todas
	 */
	List<RecursoInstitucional> ejecutar(String busqueda, Integer idCategoria);
}
