package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.application.port.in.BuscarRecursosInstitucionales;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.CategoriaDeRecursoDto;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.RecursoInstitucionalDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Guía institucional (RF11): la única parte de la aplicación que se consulta sin cuenta. */
@RestController
@RequestMapping("/api/publico/guia")
class GuiaController {

	private final ListarCategoriasDeRecurso listarCategorias;
	private final BuscarRecursosInstitucionales buscarRecursos;
	private final List<String> sugerencias;

	GuiaController(
			ListarCategoriasDeRecurso listarCategorias,
			BuscarRecursosInstitucionales buscarRecursos,
			@Value("${cundiapp.guia.sugerencias}") List<String> sugerencias) {
		this.listarCategorias = listarCategorias;
		this.buscarRecursos = buscarRecursos;
		this.sugerencias = List.copyOf(sugerencias);
	}

	@GetMapping("/categorias")
	List<CategoriaDeRecursoDto> categorias() {
		return listarCategorias.ejecutar().stream().map(CategoriaDeRecursoDto::desde).toList();
	}

	/** Busca por título y descripción; sin parámetros devuelve toda la guía agrupada por categoría. */
	@GetMapping("/recursos")
	List<RecursoInstitucionalDto> recursos(
			@RequestParam(name = "buscar", required = false) String buscar,
			@RequestParam(name = "categoria", required = false) Integer categoria) {
		return buscarRecursos.ejecutar(buscar, categoria).stream().map(RecursoInstitucionalDto::desde).toList();
	}

	/**
	 * Temas que se le proponen al estudiante para que sepa qué buscar. Son contenido editorial,
	 * así que viven en la configuración (cundiapp.guia.sugerencias) y se cambian sin tocar código.
	 */
	@GetMapping("/sugerencias")
	List<String> sugerencias() {
		return sugerencias;
	}
}
