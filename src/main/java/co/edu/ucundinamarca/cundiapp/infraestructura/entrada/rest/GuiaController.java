package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest.dto.CategoriaDeRecursoDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Guía institucional (RF11): la única parte de la aplicación que se consulta sin cuenta. */
@RestController
@RequestMapping("/api/publico/guia")
class GuiaController {

	private final ListarCategoriasDeRecurso listarCategorias;

	GuiaController(ListarCategoriasDeRecurso listarCategorias) {
		this.listarCategorias = listarCategorias;
	}

	@GetMapping("/categorias")
	List<CategoriaDeRecursoDto> categorias() {
		return listarCategorias.ejecutar().stream().map(CategoriaDeRecursoDto::desde).toList();
	}
}
