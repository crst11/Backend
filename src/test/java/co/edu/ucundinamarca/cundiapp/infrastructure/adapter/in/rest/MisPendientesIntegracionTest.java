package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
 * Mis pendientes (RF06, SCRUM-29 y SCRUM-30), con seguridad real y PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: se crean con prioridad media si no se elige otra, salen del más próximo al
 * más lejano con el servidor diciendo cuáles vencieron y cuáles vencen en menos de 48 horas, se marcan
 * como hechos y se borran, y los de una persona no los ve, ni toca, otra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MisPendientesIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String ALGEBRA = "CAD612021101";

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

	/** Una asignatura matriculada, que hoy solo se consigue importando el registro extendido. */
	private int conMatricula(String token) throws Exception {
		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
								  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":3,
								   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
								   "notas":[{"codigoAsignatura":"%s","nota":4.2}]}]}""".formatted(ALGEBRA)))
				.andExpect(status().isCreated());
		String lista = mvc.perform(get("/api/mis/matriculas").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();
		List<Integer> ids = JsonPath.read(lista, "$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(ALGEBRA));
		return ids.getFirst();
	}

	private static String cuerpo(String titulo, Instant fecha, String prioridad, Integer matricula) {
		return "{\"titulo\":\"%s\",\"fechaLimite\":\"%s\"%s%s}".formatted(titulo, fecha,
				prioridad == null ? "" : ",\"prioridad\":\"" + prioridad + "\"",
				matricula == null ? "" : ",\"idMatricula\":" + matricula);
	}

	private int crear(String token, String titulo, Duration dentroDe) throws Exception {
		MvcResult hecho = mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo(titulo, Instant.now().plus(dentroDe), null, null)))
				.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(hecho.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void creaUnPendienteConPrioridadMediaSiNoSeEligeOtra() throws Exception {
		String token = entrar("pend.crea@ucundinamarca.edu.co");

		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo("Taller de redes", Instant.now().plus(Duration.ofDays(6)), null, null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.titulo").value("Taller de redes"))
				.andExpect(jsonPath("$.prioridad").value("media"))
				.andExpect(jsonPath("$.estado").value("abierto"))
				.andExpect(jsonPath("$.situacion").value("proximo"))
				.andExpect(jsonPath("$.idMatricula").doesNotExist());
	}

	@Test
	void laListaSaleDelMasProximoAlMasLejanoYElServidorDiceCualesVencenYCualesSonUrgentes() throws Exception {
		String token = entrar("pend.orden@ucundinamarca.edu.co");
		crear(token, "Lejano", Duration.ofDays(10));
		crear(token, "Urgente", Duration.ofHours(20));
		crear(token, "Vencido", Duration.ofDays(-1));

		mvc.perform(get("/api/mis/pendientes").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$..titulo", contains("Vencido", "Urgente", "Lejano")))
				.andExpect(jsonPath("$[0].situacion").value("vencido"))
				.andExpect(jsonPath("$[1].situacion").value("urgente"))
				.andExpect(jsonPath("$[2].situacion").value("proximo"));
	}

	@Test
	void marcarUnoComoHechoLoSacaDeLaListaYSePuedeReabrir() throws Exception {
		String token = entrar("pend.hecho@ucundinamarca.edu.co");
		int id = crear(token, "Parcial", Duration.ofDays(3));
		crear(token, "Otro", Duration.ofDays(4));

		mvc.perform(patch("/api/mis/pendientes/" + id).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"cumplido\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("cumplido"))
				.andExpect(jsonPath("$.situacion").value("cumplido"));
		mvc.perform(get("/api/mis/pendientes").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$..titulo", contains("Otro")));
		mvc.perform(get("/api/mis/pendientes?incluirCumplidos=true").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$..titulo", contains("Otro", "Parcial")));

		mvc.perform(patch("/api/mis/pendientes/" + id).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"cumplido\":false}"))
				.andExpect(jsonPath("$.estado").value("abierto"));
		mvc.perform(get("/api/mis/pendientes").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$..titulo", contains("Parcial", "Otro")));
	}

	@Test
	void borrarUnoLoQuitaYBorrarlo2VecesDiceQueNoLoEncuentra() throws Exception {
		String token = entrar("pend.borra@ucundinamarca.edu.co");
		int id = crear(token, "Taller", Duration.ofDays(2));

		mvc.perform(delete("/api/mis/pendientes/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/mis/pendientes?incluirCumplidos=true").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$").isEmpty());
		mvc.perform(delete("/api/mis/pendientes/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("No encontramos ese pendiente"));
	}

	@Test
	void losPendientesDeUnaPersonaNoLosVeNiLosTocaOtra() throws Exception {
		String ana = entrar("pend.ana@ucundinamarca.edu.co");
		String luis = entrar("pend.luis@ucundinamarca.edu.co");
		int deAna = crear(ana, "De Ana", Duration.ofDays(2));

		mvc.perform(get("/api/mis/pendientes?incluirCumplidos=true").header("Authorization", "Bearer " + luis))
				.andExpect(jsonPath("$").isEmpty());
		mvc.perform(patch("/api/mis/pendientes/" + deAna).header("Authorization", "Bearer " + luis)
						.contentType(MediaType.APPLICATION_JSON).content("{\"cumplido\":true}"))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(delete("/api/mis/pendientes/" + deAna).header("Authorization", "Bearer " + luis))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(get("/api/mis/pendientes").header("Authorization", "Bearer " + ana))
				.andExpect(jsonPath("$[0].estado").value("abierto"));
	}

	@Test
	void sePuedeAsociarAUnaAsignaturaPropiaPeroNoAUnaAjena() throws Exception {
		String ana = entrar("pend.mat.ana@ucundinamarca.edu.co");
		String luis = entrar("pend.mat.luis@ucundinamarca.edu.co");
		int deAna = conMatricula(ana);
		Instant fecha = Instant.now().plus(Duration.ofDays(5));

		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + ana)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo("Parcial", fecha, "alta", deAna)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.idMatricula").value(deAna))
				.andExpect(jsonPath("$.prioridad").value("alta"));
		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + luis)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo("Parcial", fecha, null, deAna)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("No encontramos esa asignatura entre tus matrículas"));
		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + luis)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo("Parcial", fecha, null, 987654)))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(get("/api/mis/pendientes").header("Authorization", "Bearer " + luis))
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void unPendienteMalEscritoSeRechazaConUnMensajeQueSeEntiende() throws Exception {
		String token = entrar("pend.valida@ucundinamarca.edu.co");
		Instant fecha = Instant.now().plus(Duration.ofDays(2));

		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo("   ", fecha, null, null)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Escribe un título para el pendiente"));
		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo("x".repeat(121), fecha, null, null)))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Sin fecha\"}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Falta la fecha límite del pendiente"));
		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo("Taller", fecha, "urgentísima", null)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("La prioridad tiene que ser alta, media o baja"));
		mvc.perform(post("/api/mis/pendientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Taller\",\"fechaLimite\":\"mañana\"}"))
				.andExpect(status().isBadRequest());
		mvc.perform(patch("/api/mis/pendientes/1").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sinSesionNadaSeVe() throws Exception {
		mvc.perform(get("/api/mis/pendientes")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/mis/pendientes").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/mis/pendientes/1")).andExpect(status().isUnauthorized());
	}
}
