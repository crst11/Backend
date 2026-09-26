package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SCRUM-19 contra PostgreSQL real con los documentos de V4 y la seguridad real: la guía se consulta
 * sin cuenta, busca por título y descripción, y cada sugerencia configurada encuentra algo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GuiaIntegracionTest {

	@Autowired
	private MockMvc mvc;

	private List<Map<String, Object>> recursos(String parametros) throws Exception {
		String cuerpo = mvc.perform(get("/api/publico/guia/recursos" + parametros))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$");
	}

	private static List<String> titulos(List<Map<String, Object>> recursos) {
		return recursos.stream().map(r -> (String) r.get("titulo")).toList();
	}

	@Test
	void sinCuentaMuestraTodosLosDocumentosVigentesConSuFuenteOficialYAgrupadosPorCategoria() throws Exception {
		var todos = recursos("");

		assertThat(todos).hasSize(19);
		assertThat(todos).allSatisfy(recurso -> {
			assertThat((String) recurso.get("url")).startsWith("https://");
			assertThat(recurso.get("vigente")).isEqualTo(true);
			assertThat(recurso.get("fechaVerificacion")).isEqualTo("2026-09-26");
		});
		List<String> categorias = todos.stream()
				.map(r -> (String) ((Map<?, ?>) r.get("categoria")).get("nombre"))
				.distinct()
				.toList();
		assertThat(categorias).containsExactly("Reglamentos", "Trámites y calendario", "Plantillas y formatos", "Convocatorias", "Plataformas");
	}

	@Test
	void encuentraLasPlantillasYDiceQueSeDescarganYEnQueFormato() throws Exception {
		var plantillas = recursos("?buscar=plantillas");

		assertThat(titulos(plantillas)).containsExactlyInAnyOrder(
				"Plantilla de documento en Word",
				"Plantilla de hoja de cálculo en Excel",
				"Plantilla para presentaciones institucionales (2026)");
		assertThat(plantillas).extracting(r -> r.get("formato")).containsExactlyInAnyOrder("Word", "Excel", "PowerPoint");
		assertThat(plantillas).allSatisfy(r -> assertThat(r.get("descargable")).isEqualTo(true));
	}

	@Test
	void buscaComoEscribeUnaPersonaSinTildesYEnDesorden() throws Exception {
		assertThat(titulos(recursos("?buscar=materias cancelar"))).containsExactly("Adición y cancelación de materias");
		assertThat(titulos(recursos("?buscar=CALENDARIO academico"))).contains("Calendario académico 2026-2");
		assertThat(recursos("?buscar=algo que no existe")).isEmpty();
	}

	@Test
	void filtraPorCategoria() throws Exception {
		String cuerpo = mvc.perform(get("/api/publico/guia/categorias")).andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(cuerpo, "$[?(@.nombre == 'Plataformas')].id");

		assertThat(titulos(recursos("?categoria=" + ids.getFirst())))
				.containsExactly("Correo institucional", "Plataforma institucional");
	}

	@Test
	void cadaSugerenciaConfiguradaEncuentraAlMenosUnDocumento() throws Exception {
		String cuerpo = mvc.perform(get("/api/publico/guia/sugerencias")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		List<String> sugerencias = JsonPath.read(cuerpo, "$");

		assertThat(sugerencias).contains("Plantillas", "Calendario académico", "Cancelar materias");
		for (String sugerencia : sugerencias) {
			assertThat(recursos("?buscar=" + sugerencia)).as("la sugerencia «%s» encuentra algo", sugerencia).isNotEmpty();
		}
	}

	@Test
	void unaBusquedaDemasiadoLargaSeRechazaConProblemDetail() throws Exception {
		mvc.perform(get("/api/publico/guia/recursos").param("buscar", "a".repeat(81)))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(get("/api/publico/guia/recursos").param("categoria", "no-es-numero"))
				.andExpect(status().isBadRequest());
	}
}
