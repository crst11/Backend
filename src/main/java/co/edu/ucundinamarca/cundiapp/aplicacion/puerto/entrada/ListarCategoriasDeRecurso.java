package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CategoriaDeRecurso;
import java.util.List;

/** Caso de uso: listar las categorías de la guía institucional (RF11). */
public interface ListarCategoriasDeRecurso {

	List<CategoriaDeRecurso> ejecutar();
}
