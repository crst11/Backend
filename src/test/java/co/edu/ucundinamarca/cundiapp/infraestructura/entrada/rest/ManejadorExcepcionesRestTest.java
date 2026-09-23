package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifica que una violación de regla de negocio se traduce a un ProblemDetail 422. */
@WebMvcTest(GuiaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManejadorExcepcionesRestTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ListarCategoriasDeRecurso listarCategorias;

	@Test
	void traduceLaExcepcionDeDominioAProblemDetail() throws Exception {
		given(listarCategorias.ejecutar())
				.willThrow(new ReglaDeNegocioVioladaException("La categoría necesita un nombre"));

		mvc.perform(get("/api/publico/guia/categorias"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(content().json("""
						{"title":"Violación de regla de negocio","detail":"La categoría necesita un nombre","status":422}"""));
	}
}
