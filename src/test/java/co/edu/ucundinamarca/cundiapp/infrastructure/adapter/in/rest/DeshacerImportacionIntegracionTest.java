package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Deshacer la última carga y ver el historial de importaciones (SCRUM-24).
 *
 * <p>Lo que de verdad hay que demostrar: deshacer no es borrar. Lo que la carga creó se elimina,
 * pero lo que pisó vuelve a la nota que tenía antes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeshacerImportacionIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	private String entrar(String correo) throws Exception {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true,
						Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		MvcResult login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	private void elegirPrograma(String token) throws Exception {
		mvc.perform(put("/api/mis/perfil-academico")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
	}

	/** Una carga con una sola asignatura, para que la nota sea fácil de seguir. */
	private int importar(String token, String nota, String promedio) throws Exception {
		String cuerpo = """
				{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
				  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":3,
				   "promedioPeriodo":%s,"promedioAcumulado":%s,
				   "notas":[{"codigoAsignatura":"CAD612021101","nota":%s}]}]}"""
				.formatted(promedio, promedio, nota);
		MvcResult respuesta = mvc.perform(post("/api/mis/importaciones")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(respuesta.getResponse().getContentAsString(), "$.idImportacion");
	}

	private String historial(String token) throws Exception {
		return mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void muestraElHistorialDeCargasConSuResumen() throws Exception {
		String token = entrar("deshacer.resumen@ucundinamarca.edu.co");
		elegirPrograma(token);
		importar(token, "4.2", "4.2");

		mvc.perform(get("/api/mis/importaciones").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombreArchivo").value("registro_extendido.pdf"))
				.andExpect(jsonPath("$[0].estado").value("confirmada"))
				.andExpect(jsonPath("$[0].detectadas").value(1))
				.andExpect(jsonPath("$[0].confirmadas").value(1))
				.andExpect(jsonPath("$[0].sePuedeDeshacer").value(true));
	}

	@Test
	void deshacerQuitaLoQueLaCargaCreo() throws Exception {
		String token = entrar("deshacer.creadas@ucundinamarca.edu.co");
		elegirPrograma(token);
		int idImportacion = importar(token, "4.2", "4.2");

		mvc.perform(post("/api/mis/importaciones/" + idImportacion + "/reversion")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eliminadas").value(1))
				.andExpect(jsonPath("$.restauradas").value(0));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.periodos.length()").value(0));
	}

	@Test
	void deshacerDevuelveLaNotaQueLaCargaPiso_noLaBorra() throws Exception {
		// Esto es lo que separa un "deshacer" de un "borrar la carga".
		String token = entrar("deshacer.pisadas@ucundinamarca.edu.co");
		elegirPrograma(token);
		importar(token, "4.2", "4.2");
		int segunda = importar(token, "3.0", "3.0");

		mvc.perform(post("/api/mis/importaciones/" + segunda + "/reversion")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eliminadas").value(0))
				.andExpect(jsonPath("$.restauradas").value(1));

		String historial = historial(token);
		org.assertj.core.api.Assertions
				.assertThat(JsonPath.<Double>read(historial, "$.periodos[0].asignaturas[0].nota"))
				.isEqualTo(4.2);
		// El resumen oficial también vuelve al de la primera carga.
		org.assertj.core.api.Assertions
				.assertThat(JsonPath.<Double>read(historial, "$.periodos[0].promedio"))
				.isEqualTo(4.2);
	}

	@Test
	void unaCargaDeshechaNoSePuedeDeshacerDeNuevo() throws Exception {
		String token = entrar("deshacer.dosveces@ucundinamarca.edu.co");
		elegirPrograma(token);
		int idImportacion = importar(token, "4.2", "4.2");
		mvc.perform(post("/api/mis/importaciones/" + idImportacion + "/reversion")
				.header("Authorization", "Bearer " + token)).andExpect(status().isOk());

		mvc.perform(post("/api/mis/importaciones/" + idImportacion + "/reversion")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ya se deshizo")));
	}

	@Test
	void soloSePuedeDeshacerLaUltimaCarga() throws Exception {
		String token = entrar("deshacer.lavieja@ucundinamarca.edu.co");
		elegirPrograma(token);
		int primera = importar(token, "4.2", "4.2");
		importar(token, "3.0", "3.0");

		mvc.perform(post("/api/mis/importaciones/" + primera + "/reversion")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("última carga")));
	}

	@Test
	void despuesDeDeshacerLaAnteriorVuelveAPoderDeshacerse() throws Exception {
		String token = entrar("deshacer.encadena@ucundinamarca.edu.co");
		elegirPrograma(token);
		int primera = importar(token, "4.2", "4.2");
		int segunda = importar(token, "3.0", "3.0");

		mvc.perform(post("/api/mis/importaciones/" + segunda + "/reversion")
				.header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones/" + primera + "/reversion")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eliminadas").value(1));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.periodos.length()").value(0));
	}

	@Test
	void nadieDeshaceLaCargaDeOtroEstudiante() throws Exception {
		String ajeno = entrar("deshacer.ajeno@ucundinamarca.edu.co");
		elegirPrograma(ajeno);
		int deOtro = importar(ajeno, "4.2", "4.2");
		String token = entrar("deshacer.intruso@ucundinamarca.edu.co");

		mvc.perform(post("/api/mis/importaciones/" + deOtro + "/reversion")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("No encontramos")));
	}

	@Test
	void sinSesionNoSeVeNiSeDeshaceNingunaCarga() throws Exception {
		mvc.perform(get("/api/mis/importaciones")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/mis/importaciones/1/reversion")).andExpect(status().isUnauthorized());
	}
}
