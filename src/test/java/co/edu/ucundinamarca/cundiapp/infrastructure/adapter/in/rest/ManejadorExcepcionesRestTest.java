package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoNoEnviadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifica que las excepciones del dominio se traducen al ProblemDetail que les corresponde. */
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

	@Test
	void unCorreoQueNoSalioEsUnServicioNoDisponibleConSuPropioTitulo() throws Exception {
		given(listarCategorias.ejecutar()).willThrow(new CorreoNoEnviadoException(
				"No pudimos enviar el código a tu correo. Intenta de nuevo en unos minutos", new IllegalStateException()));

		mvc.perform(get("/api/publico/guia/categorias"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().json("""
						{"title":"Correo no enviado","status":503,
						 "detail":"No pudimos enviar el código a tu correo. Intenta de nuevo en unos minutos"}"""));
	}
}
