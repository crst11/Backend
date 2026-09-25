package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.application.port.out.CategoriaDeRecursoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeRecurso;
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
