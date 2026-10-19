package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.application.port.in.BuscarRecursosInstitucionales;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarCategoriasDeRecurso;
import co.edu.ucundinamarca.cundiapp.application.port.out.LimitadorDePeticionesPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.CorreoNoEnviadoException;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

	/** El filtro de correos por IP lo pide; el adaptador de la base de datos no forma parte de la porción web. */
	@MockitoBean
	private LimitadorDePeticionesPort limitadorDePeticiones;

	@MockitoBean
	private ListarCategoriasDeRecurso listarCategorias;

	@MockitoBean
	private BuscarRecursosInstitucionales buscarRecursos;

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
	void unaViolacionDeIntegridadEsUn422ConUnMensajeGenericoSinElDetalleDeLaBase() throws Exception {
		given(listarCategorias.ejecutar()).willThrow(new DataIntegrityViolationException(
				"PreparedStatementCallback; ERROR: new row for relation \"resumen_periodo\" violates check "
						+ "constraint \"ck_resumen_creditos\" Detail: Failing row contains (7, 2024-2, 20)",
				new SQLException("violates check constraint \"ck_resumen_creditos\"", "23514")));

		mvc.perform(get("/api/publico/guia/categorias"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(content().json("""
						{"title":"Violación de regla del modelo","status":422,
						 "detail":"No pudimos guardar: los datos no cumplen una regla del modelo"}"""))
				.andExpect(content().string(not(containsString("ck_resumen_creditos"))))
				.andExpect(content().string(not(containsString("resumen_periodo"))))
				.andExpect(content().string(not(containsString("Failing row"))));
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
