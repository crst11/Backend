package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Actividades extra en el horario y en Hoy (SCRUM-266), con seguridad real y PostgreSQL real.
 *
 * <p>Lo que hay que demostrar: una actividad con varios días se guarda como una por día, aparece en Mi horario y en
 * Hoy como una clase más (marcada como extra, sin matrícula), se quita sola, no se puede quitar la de otro, y lo
 * que no tiene sentido se rechaza con un mensaje útil. El martes 13 de octubre de 2026 es lectivo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActividadesExtraIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String RUTA = "/api/mis/actividades-extra";

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

	private ResultActions anadir(String token, String cuerpo) throws Exception {
		return mvc.perform(post(RUTA)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo));
	}

	private static String baile(String dias, String desde, String hasta) {
		return """
				{"nombre":"Baile","dias":%s,"horaInicio":"%s","horaFin":"%s","lugar":"Coliseo","responsable":"Profe Ana"}"""
				.formatted(dias, desde, hasta);
	}

	@Test
	void unaActividadConVariosDiasSeGuardaUnaPorDiaYApareceEnMiHorarioComoExtra() throws Exception {
		String token = entrar("extra.horario@ucundinamarca.edu.co");

		anadir(token, baile("[\"lunes\",\"miercoles\"]", "17:00", "19:00"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].idActividadExtra").isNumber())
				.andExpect(jsonPath("$[0].esExtra").value(true))
				.andExpect(jsonPath("$[0].idMatricula").doesNotExist())
				.andExpect(jsonPath("$[0].asignatura").value("Baile"))
				.andExpect(jsonPath("$[0].lugar").value("Coliseo"))
				.andExpect(jsonPath("$[0].responsable").value("Profe Ana"));

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalDeClases").value(2))
				.andExpect(jsonPath("$.dias[0].dia").value("lunes"))
				.andExpect(jsonPath("$.dias[0].clases[0].esExtra").value(true))
				.andExpect(jsonPath("$.dias[0].clases[0].horaInicio").value("17:00"))
				.andExpect(jsonPath("$.dias[1].dia").value("miercoles"))
				// No es una asignatura: no sale en las que se pueden elegir para filtrar.
				.andExpect(jsonPath("$.asignaturas.length()").value(0));
	}

	@Test
	void apareceEnHoyIgualQueUnaClaseSinNotasNiMatricula() throws Exception {
		String token = entrar("extra.hoy@ucundinamarca.edu.co");
		anadir(token, baile("[\"martes\"]", "17:00", "19:00")).andExpect(status().isCreated());

		mvc.perform(get("/api/mis/dia").param("fecha", "2026-10-13").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clases.length()").value(1))
				.andExpect(jsonPath("$.clases[0].asignatura").value("Baile"))
				.andExpect(jsonPath("$.clases[0].esExtra").value(true))
				.andExpect(jsonPath("$.clases[0].idMatricula").doesNotExist())
				.andExpect(jsonPath("$.clases[0].actividades").value(0))
				.andExpect(jsonPath("$.clases[0].horaInicio").value("17:00"));
	}

	@Test
	void sePuedeQuitarYLaDeOtroEstudianteNoSeEncuentra() throws Exception {
		String dueno = entrar("extra.dueno@ucundinamarca.edu.co");
		String otro = entrar("extra.otro@ucundinamarca.edu.co");
		MvcResult creada = anadir(dueno, baile("[\"jueves\"]", "16:00", "18:00")).andExpect(status().isCreated()).andReturn();
		int id = JsonPath.read(creada.getResponse().getContentAsString(), "$[0].idActividadExtra");

		mvc.perform(delete(RUTA + "/" + id).header("Authorization", "Bearer " + otro))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("No encontramos esa actividad"));

		mvc.perform(delete(RUTA + "/" + id).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + dueno))
				.andExpect(jsonPath("$.totalDeClases").value(0));
		mvc.perform(delete(RUTA + "/" + id).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void loQueNoTieneSentidoSeRechazaConUnMensajeUtil() throws Exception {
		String token = entrar("extra.invalidas@ucundinamarca.edu.co");

		anadir(token, baile("[]", "17:00", "19:00"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Elige al menos un día"));
		anadir(token, baile("[\"lunes\"]", "19:00", "17:00")).andExpect(status().isUnprocessableEntity());
		anadir(token, baile("[\"funday\"]", "17:00", "19:00")).andExpect(status().isUnprocessableEntity());
		// Textos y días desmesurados se frenan en la puerta, sin llegar al dominio.
		anadir(token, baile("[\"" + "x".repeat(5000) + "\"]", "17:00", "19:00")).andExpect(status().isBadRequest());
		anadir(token, "{\"nombre\":\"" + "n".repeat(5000)
						+ "\",\"dias\":[\"lunes\"],\"horaInicio\":\"17:00\",\"horaFin\":\"19:00\"}")
				.andExpect(status().isBadRequest());
		anadir(token, "{\"nombre\":\"  \",\"dias\":[\"lunes\"],\"horaInicio\":\"17:00\",\"horaFin\":\"19:00\"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Escribe el nombre de la actividad"));
		anadir(token, "{\"nombre\":\"Baile\",\"dias\":[\"lunes\"]}").andExpect(status().isBadRequest());
	}

	@Test
	void noPasaDelLimiteDeVeinte() throws Exception {
		String token = entrar("extra.limite@ucundinamarca.edu.co");
		String sieteDias = "[\"lunes\",\"martes\",\"miercoles\",\"jueves\",\"viernes\",\"sabado\",\"domingo\"]";
		anadir(token, baile(sieteDias, "08:00", "09:00")).andExpect(status().isCreated());
		anadir(token, baile(sieteDias, "10:00", "11:00")).andExpect(status().isCreated());

		anadir(token, baile(sieteDias, "12:00", "13:00"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Puedes tener hasta 20 actividades extra"));
		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(14));
	}

	@Test
	void sinSesionNoSeAnadeNiSeQuita() throws Exception {
		mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(baile("[\"lunes\"]", "17:00", "19:00")))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete(RUTA + "/1")).andExpect(status().isUnauthorized());
	}
}
