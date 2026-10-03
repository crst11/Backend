package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Elegir programa y ver el plan por período, con la seguridad real y PostgreSQL real (SCRUM-21). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MiPlanDeEstudiosIntegracionTest {

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
		MvcResult login = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	@Test
	void muestraLosProgramasEntreLosQueElegir() throws Exception {
		String token = entrar("plan.programas@ucundinamarca.edu.co");

		mvc.perform(get("/api/programas").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].codigo").value(FUSAGASUGA))
				.andExpect(jsonPath("$[0].sede").value("Fusagasugá"))
				.andExpect(jsonPath("$[0].totalCreditos").value(153))
				.andExpect(jsonPath("$[0].numeroPeriodos").value(9));
	}

	@Test
	void elPlanLlegaAgrupadoPorPeriodoYConLosPrerrequisitos() throws Exception {
		String token = entrar("plan.ruta@ucundinamarca.edu.co");

		mvc.perform(get("/api/programas/" + FUSAGASUGA + "/plan").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos.length()").value(9))
				.andExpect(jsonPath("$.periodos[0].periodo").value(1))
				.andExpect(jsonPath("$.periodos[0].creditos").value(16))
				// Dentro de cada período van en orden alfabético: Álgebra Lineal abre el plan.
				.andExpect(jsonPath("$.periodos[0].asignaturas[0].nombre").value("Álgebra Lineal"))
				.andExpect(jsonPath("$.periodos[0].asignaturas[0].prerrequisitos.length()").value(0))
				// Programación I (período 2) exige Pensamiento Algorítmico.
				.andExpect(jsonPath("$.periodos[1].creditos").value(18));
	}

	@Test
	void sinElegirProgramaElPerfilLoDiceEnVezDeFallar() throws Exception {
		String token = entrar("plan.sinelegir@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/perfil-academico").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.programaElegido").value(false))
				.andExpect(jsonPath("$.plan").doesNotExist());
	}

	@Test
	void alElegirProgramaElPerfilQuedaConSuPlanYSePuedeCambiarDespues() throws Exception {
		String token = entrar("plan.elegir@ucundinamarca.edu.co");

		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.programaElegido").value(true))
				.andExpect(jsonPath("$.plan.programa.sede").value("Fusagasugá"));

		// Queda guardado: una consulta posterior lo devuelve sin volver a elegir.
		mvc.perform(get("/api/mis/perfil-academico").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.programaElegido").value(true))
				.andExpect(jsonPath("$.plan.periodos.length()").value(9));

		// Elegir otra vez el mismo programa no rompe nada: PUT es repetible.
		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
	}

	@Test
	void unProgramaQueNoExisteSeRechaza() throws Exception {
		String token = entrar("plan.inexistente@ucundinamarca.edu.co");

		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"000000\"}"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void elCatalogoExigeSesion() throws Exception {
		mvc.perform(get("/api/programas")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/mis/perfil-academico")).andExpect(status().isUnauthorized());
	}
}
