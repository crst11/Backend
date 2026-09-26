package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.application.port.in.BuscarRecursosInstitucionales;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeRecurso;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de la capa web en aislamiento: verifica que el controlador traduce el resultado del
 * caso de uso a DTOs y nunca expone el modelo de dominio. Las reglas de seguridad (que esta ruta
 * es pública y las demás no) las cubre SeguridadConfig, no este slice.
 */
@WebMvcTest(GuiaController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuiaControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ListarCategoriasDeRecurso listarCategorias;

	@MockitoBean
	private BuscarRecursosInstitucionales buscarRecursos;

	@Test
	void respondeLasCategoriasComoJson() throws Exception {
		given(listarCategorias.ejecutar()).willReturn(List.of(
				new CategoriaDeRecurso(1, "Reglamentos"),
				new CategoriaDeRecurso(2, "Formatos")));

		mvc.perform(get("/api/publico/guia/categorias"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						[{"id":1,"nombre":"Reglamentos"},{"id":2,"nombre":"Formatos"}]"""));
	}
}
