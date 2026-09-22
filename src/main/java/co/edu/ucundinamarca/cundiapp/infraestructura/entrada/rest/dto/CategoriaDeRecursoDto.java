package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CategoriaDeRecurso;

/** El dominio no se filtra al JSON: el controlador siempre responde con un DTO. */
public record CategoriaDeRecursoDto(Integer id, String nombre) {

	public static CategoriaDeRecursoDto desde(CategoriaDeRecurso categoria) {
		return new CategoriaDeRecursoDto(categoria.id(), categoria.nombre());
	}
}
