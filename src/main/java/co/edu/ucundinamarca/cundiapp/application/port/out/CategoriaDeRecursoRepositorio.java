package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeRecurso;
import java.util.List;

/** Lo que el núcleo necesita del mundo para leer las categorías de la guía institucional. */
public interface CategoriaDeRecursoRepositorio {

	List<CategoriaDeRecurso> buscarTodas();
}
