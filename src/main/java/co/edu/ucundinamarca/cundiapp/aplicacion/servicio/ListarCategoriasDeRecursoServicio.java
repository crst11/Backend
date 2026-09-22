package co.edu.ucundinamarca.cundiapp.aplicacion.servicio;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CategoriaDeRecurso;
import java.util.List;

/** Sin anotaciones de Spring: la capa de aplicación no depende del framework. */
public class ListarCategoriasDeRecursoServicio implements ListarCategoriasDeRecurso {

	private final CategoriaDeRecursoRepositorio repositorio;

	public ListarCategoriasDeRecursoServicio(CategoriaDeRecursoRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public List<CategoriaDeRecurso> ejecutar() {
		return repositorio.buscarTodas();
	}
}
