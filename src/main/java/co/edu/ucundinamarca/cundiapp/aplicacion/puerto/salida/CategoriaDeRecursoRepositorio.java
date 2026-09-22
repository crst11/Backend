package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CategoriaDeRecurso;
import java.util.List;

/** Lo que el núcleo necesita del mundo para leer las categorías de la guía institucional. */
public interface CategoriaDeRecursoRepositorio {

	List<CategoriaDeRecurso> buscarTodas();
}
