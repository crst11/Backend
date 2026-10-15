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
 * La pantalla del día de extremo a extremo, con seguridad real y PostgreSQL real (SCRUM-31). Las fechas
 * se piden explícitas para que la prueba no dependa del día en que corra: el lunes 12 de octubre de
 * 2026 es festivo y el martes 13 es lectivo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MiDiaIntegracionTest {

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

	/** Dos asignaturas en curso: una con clase el martes y otra el miércoles, con salón y docente. */
	private String conHorario(String correo) throws Exception {
		String token = entrar(correo);
		mvc.perform(put("/api/mis/perfil-academico").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
		mvc.perform(post("/api/mis/importaciones/horario").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"periodo":"2026-2","asignaturas":[
								  {"codigo":"CAD612021519","grupo":"SIS.FUSA 501M","clases":[
								    {"dia":"martes","horaInicio":"09:00","horaFin":"11:00","salon":"E-302",
								     "bloque":"BLOQUE E","sede":"CAMPUS","docente":{"nombres":"Ana","apellidos":"Gomez"}}]},
								  {"codigo":"CAD612021520","grupo":"SIS.FUSA 501M","clases":[
								    {"dia":"miercoles","horaInicio":"07:00","horaFin":"09:00","salon":"A-102","sede":"CAMPUS"}]}]}"""))
				.andExpect(status().isCreated());
		return token;
	}

	@Test
	void unDiaLectivoTraeSusClasesConSalonYDocente() throws Exception {
		String token = conHorario("dia.lectivo@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/dia?fecha=2026-10-13").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fecha").value("2026-10-13"))
				.andExpect(jsonPath("$.dia").value("martes"))
				.andExpect(jsonPath("$.tipo").value("lectivo"))
				.andExpect(jsonPath("$.clases.length()").value(1))
				.andExpect(jsonPath("$.clases[0].asignatura").value("Ecuaciones Diferenciales"))
				.andExpect(jsonPath("$.clases[0].horaInicio").value("09:00"))
				.andExpect(jsonPath("$.clases[0].salon").value("E-302"))
				.andExpect(jsonPath("$.clases[0].docente.apellidos").value("Gomez"))
				.andExpect(jsonPath("$.clases[0].actividades").value(0))
				.andExpect(jsonPath("$.siguiente.fecha").value("2026-10-14"))
				.andExpect(jsonPath("$.siguiente.clases[0].asignatura").value("Sistemas Operativos"));
	}

	@Test
	void unFestivoNoMuestraClasesYApuntaAlMartes() throws Exception {
		String token = conHorario("dia.festivo@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/dia?fecha=2026-10-12").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dia").value("lunes"))
				.andExpect(jsonPath("$.tipo").value("festivo"))
				.andExpect(jsonPath("$.festivo").value("Día de la Raza"))
				.andExpect(jsonPath("$.clases.length()").value(0))
				.andExpect(jsonPath("$.siguiente.fecha").value("2026-10-13"))
				.andExpect(jsonPath("$.siguiente.dia").value("martes"))
				.andExpect(jsonPath("$.siguiente.clases.length()").value(1));
	}

	@Test
	void sinFechaEsHoyDeBogota() throws Exception {
		String token = conHorario("dia.hoy@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/dia").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hoy").isString())
				.andExpect(jsonPath("$.fecha").isString());
	}

	@Test
	void sinHorarioLaPantallaVieneVaciaYSinSiguiente() throws Exception {
		String token = entrar("dia.vacio@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/dia?fecha=2026-10-13").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clases.length()").value(0))
				.andExpect(jsonPath("$.siguiente").doesNotExist());
	}

	@Test
	void unaFechaMalEscritaEsUnaPeticionMalArmada() throws Exception {
		String token = entrar("dia.formato@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/dia?fecha=13-10-2026").header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest());
	}

	@Test
	void elHorarioEsDeCadaEstudiante() throws Exception {
		conHorario("dia.mio@ucundinamarca.edu.co");
		String otro = entrar("dia.otro@ucundinamarca.edu.co");

		mvc.perform(get("/api/mis/dia?fecha=2026-10-13").header("Authorization", "Bearer " + otro))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clases.length()").value(0));
	}

	@Test
	void sinSesionNoSeVe() throws Exception {
		mvc.perform(get("/api/mis/dia")).andExpect(status().isUnauthorized());
	}
}
