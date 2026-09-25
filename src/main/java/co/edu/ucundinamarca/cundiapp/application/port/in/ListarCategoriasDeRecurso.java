package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeRecurso;
import java.util.List;

/** Caso de uso: listar las categorías de la guía institucional (RF11). */
public interface ListarCategoriasDeRecurso {

	List<CategoriaDeRecurso> ejecutar();
}
