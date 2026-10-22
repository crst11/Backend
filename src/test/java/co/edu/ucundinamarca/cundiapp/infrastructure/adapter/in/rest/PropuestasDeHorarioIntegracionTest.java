package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Acomodar el horario para matricular (RF13, SCRUM-208), con seguridad real y PostgreSQL real. El cálculo no
 * guarda nada: lo que hay que demostrar es que exige sesión, que devuelve las combinaciones sin choques en el
 * orden que pide el estudiante, que explica por qué cuando no hay ninguna y que rechaza lo inválido con el
 * código que toca.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PropuestasDeHorarioIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String RUTA = "/api/mis/horario/propuestas";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	private String entrar(String usuario) throws Exception {
		String correo = usuario + "@ucundinamarca.edu.co";
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true, Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		MvcResult login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	private ResultActions proponer(String token, String cuerpo) throws Exception {
		return mvc.perform(post(RUTA).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private static String bloque(String dia, String inicio, String fin) {
		return "{\"dia\":\"%s\",\"inicio\":\"%s\",\"fin\":\"%s\"}".formatted(dia, inicio, fin);
	}

	private static String grupo(String codigo, String... bloques) {
		return "{\"codigo\":\"%s\",\"bloques\":[%s]}".formatted(codigo, String.join(",", bloques));
	}

	private static String materia(String nombre, String... grupos) {
		return "{\"nombre\":\"%s\",\"grupos\":[%s]}".formatted(nombre, String.join(",", grupos));
	}

	private static String cuerpo(String criterio, String... materias) {
		return "{\"asignaturas\":[%s]%s}".formatted(String.join(",", materias), criterio == null ? "" : ",\"criterio\":" + criterio);
	}

	// --- Lo que funciona -------------------------------------------------------------------------------------

	@Test
	void proponeLasCombinacionesSinChoquesYLasOrdenaPorLaFranjaPedida() throws Exception {
		String token = entrar("horario.manana");

		proponer(token, cuerpo("{\"franja\":\"manana\",\"pocosHuecos\":true,\"diasLibres\":[\"viernes\"]}",
				materia("Cálculo", grupo("1", bloque("lunes", "14:00", "16:00")), grupo("2", bloque("lunes", "08:00", "10:00"))),
				materia("Física", grupo("1", bloque("lunes", "10:00", "12:00")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propuestas.length()").value(2))
				.andExpect(jsonPath("$.propuestas[0].puntaje").value(100))
				.andExpect(jsonPath("$.propuestas[0].elecciones[0].asignatura").value("Cálculo"))
				.andExpect(jsonPath("$.propuestas[0].elecciones[0].grupo").value("2"))
				.andExpect(jsonPath("$.propuestas[0].elecciones[0].bloques[0].dia").value("lunes"))
				.andExpect(jsonPath("$.propuestas[0].elecciones[0].bloques[0].inicio").value("08:00"))
				.andExpect(jsonPath("$.propuestas[0].elecciones[0].bloques[0].fin").value("10:00"))
				.andExpect(jsonPath("$.propuestas[0].resumen.minutosEnLaManana").value(240))
				.andExpect(jsonPath("$.propuestas[0].resumen.minutosDeHuecos").value(0))
				.andExpect(jsonPath("$.propuestas[0].resumen.diasLibres", Matchers.hasItem("viernes")))
				.andExpect(jsonPath("$.propuestas[0].motivos[0]").value("Sin choques entre tus 2 materias."))
				.andExpect(jsonPath("$.propuestas[1].puntaje").value(Matchers.lessThan(100)))
				.andExpect(jsonPath("$.choques.length()").value(0))
				.andExpect(jsonPath("$.mensaje").doesNotExist())
				.andExpect(jsonPath("$.busquedaIncompleta").value(false));
	}

	@Test
	void sinCriterioTodasLasCombinacionesValenCien() throws Exception {
		String token = entrar("horario.sin.criterio");

		proponer(token, cuerpo(null,
				materia("Cálculo", grupo("1", bloque("lunes", "08:00", "10:00")), grupo("2", bloque("martes", "08:00", "10:00")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propuestas.length()").value(2))
				.andExpect(jsonPath("$.propuestas[0].puntaje").value(100))
				.andExpect(jsonPath("$.propuestas[1].puntaje").value(100));
	}

	@Test
	void sinCombinacionPosibleExplicaQueMateriasChocan() throws Exception {
		String token = entrar("horario.choque");

		proponer(token, cuerpo(null,
				materia("Cálculo", grupo("1", bloque("lunes", "08:00", "10:00"))),
				materia("Física", grupo("1", bloque("lunes", "09:00", "11:00")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propuestas.length()").value(0))
				.andExpect(jsonPath("$.choques[0].asignatura").value("Cálculo"))
				.andExpect(jsonPath("$.choques[0].otra").value("Física"))
				.andExpect(jsonPath("$.mensaje").value(Matchers.containsString("todos sus grupos chocan")));
	}

	@Test
	void unaPeticionEnormeTerminaRapidoYDiceQueLaBusquedaQuedoIncompleta() throws Exception {
		String token = entrar("horario.enorme");
		String[] materias = IntStream.range(0, 10)
				.mapToObj(m -> materia("M" + m, IntStream.range(0, 12)
						.mapToObj(g -> grupo("g" + g, bloque("lunes", "%02d:00".formatted(6 + m), "%02d:30".formatted(6 + m))))
						.toArray(String[]::new)))
				.toArray(String[]::new);

		proponer(token, cuerpo("{\"franja\":\"manana\",\"pocosHuecos\":true}", materias))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propuestas.length()").value(3))
				.andExpect(jsonPath("$.busquedaIncompleta").value(true));
	}

	// --- Lo que se rechaza -----------------------------------------------------------------------------------

	// --- «Quiero tener un día libre» (SCRUM-251) -------------------------------------------------------------

	@Test
	void conDiaLibreDevuelveTodasLasOpcionesQueDejanAlgunDiaLibre() throws Exception {
		String token = entrar("horario.dia.libre");

		proponer(token, cuerpo("{\"diaLibre\":true}",
				materia("A", grupo("G1", bloque("lunes", "07:00", "09:00")), grupo("G2", bloque("martes", "07:00", "09:00"))),
				materia("B", grupo("G1", bloque("lunes", "09:00", "11:00")), grupo("G2", bloque("miercoles", "07:00", "09:00"))),
				materia("C", grupo("G1", bloque("lunes", "11:00", "13:00")), grupo("G2", bloque("jueves", "07:00", "09:00")))))
				.andExpect(status().isOk())
				// Ocho conjuntos distintos de días libres: primero los cuatro repartidos que liberan un día, después los de varios.
				.andExpect(jsonPath("$.propuestas.length()").value(8))
				.andExpect(jsonPath("$.propuestas[0].puntaje").value(100))
				.andExpect(jsonPath("$.propuestas[0].resumen.diasLiberados.length()").value(1))
				.andExpect(jsonPath("$.propuestas[3].resumen.diasLiberados.length()").value(1))
				.andExpect(jsonPath("$.propuestas[4].resumen.diasLiberados.length()").value(2))
				.andExpect(jsonPath("$.propuestas[7].resumen.diasLiberados.length()").value(3))
				.andExpect(jsonPath("$.propuestas[7].puntaje").value(33))
				.andExpect(jsonPath("$.propuestas[0].motivos", Matchers.hasItem(Matchers.startsWith("Te quedan libres: "))));
	}

	@Test
	void sinDiaLibreElCuerpoDeSiempreSigueFuncionandoYTraeLosDiasLiberados() throws Exception {
		String token = entrar("horario.sin.dia.libre");

		proponer(token, cuerpo(null,
				materia("Cálculo", grupo("1", bloque("lunes", "08:00", "10:00")), grupo("2", bloque("martes", "08:00", "10:00")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propuestas.length()").value(2))
				.andExpect(jsonPath("$.propuestas[0].puntaje").value(100))
				.andExpect(jsonPath("$.propuestas[0].resumen.diasLiberados.length()").value(1));
	}

	@Test
	void unDiaLibreQueNoEsBooleanoResponde400() throws Exception {
		String token = entrar("horario.dia.libre.malo");

		proponer(token, cuerpo("{\"diaLibre\":\"quizás\"}",
				materia("Cálculo", grupo("1", bloque("lunes", "08:00", "10:00")))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void siNingunDiaSePuedeLiberarResponde200ConElMensaje() throws Exception {
		String token = entrar("horario.dia.libre.nada");

		proponer(token, cuerpo("{\"diaLibre\":true}",
				materia("A", grupo("1", bloque("lunes", "08:00", "10:00"))),
				materia("B", grupo("1", bloque("martes", "08:00", "10:00")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propuestas.length()").value(0))
				.andExpect(jsonPath("$.mensaje").value(Matchers.containsString("no se puede dejar ningún día libre")));
	}

	@Test
	void sinSesionResponde401() throws Exception {
		mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo(null, materia("Cálculo", grupo("1", bloque("lunes", "08:00", "10:00"))))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinElCampoDeMateriasResponde400() throws Exception {
		String token = entrar("horario.sin.materias");

		proponer(token, "{}").andExpect(status().isBadRequest());
	}

	@Test
	void unaHoraQueNoEsUnaHoraResponde400() throws Exception {
		String token = entrar("horario.hora.mala");

		proponer(token, cuerpo(null, materia("Cálculo", grupo("1", bloque("lunes", "ocho", "10:00")))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unaReglaDeNegocioRotaResponde422ConElMotivo() throws Exception {
		String token = entrar("horario.reglas");

		// La clase termina antes de empezar.
		proponer(token, cuerpo(null, materia("Cálculo", grupo("1", bloque("lunes", "10:00", "08:00")))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("empezar antes de terminar")));
		// Un día que no existe.
		proponer(token, cuerpo(null, materia("Cálculo", grupo("1", bloque("lunez", "08:00", "10:00")))))
				.andExpect(status().isUnprocessableEntity());
		// Un grupo sin bloques.
		proponer(token, cuerpo(null, materia("Cálculo", grupo("1"))))
				.andExpect(status().isUnprocessableEntity());
		// Una materia repetida.
		proponer(token, cuerpo(null,
				materia("Cálculo", grupo("1", bloque("lunes", "08:00", "10:00"))),
				materia("cálculo", grupo("1", bloque("martes", "08:00", "10:00")))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("se repite")));
		// Una franja que no existe.
		proponer(token, cuerpo("{\"franja\":\"madrugada\"}", materia("Cálculo", grupo("1", bloque("lunes", "08:00", "10:00")))))
				.andExpect(status().isUnprocessableEntity());
		// Sin ninguna materia.
		proponer(token, "{\"asignaturas\":[]}").andExpect(status().isUnprocessableEntity());
	}

	@Test
	void masDeDiezMateriasResponde422() throws Exception {
		String token = entrar("horario.once");
		String[] once = IntStream.range(0, 11)
				.mapToObj(m -> materia("M" + m, grupo("1", bloque("lunes", "%02d:00".formatted(6 + m), "%02d:30".formatted(6 + m)))))
				.toArray(String[]::new);

		proponer(token, cuerpo(null, once))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(Matchers.containsString("10")));
	}

	@Test
	void masDeVeinteGruposOMasDeOchoBloquesResponde422SinArmarMillonesDeObjetos() throws Exception {
		String token = entrar("horario.limites");
		String trece = IntStream.range(0, 21)
				.mapToObj(g -> grupo("g" + g, bloque("lunes", "08:00", "10:00")))
				.collect(Collectors.joining(","));
		String nueveBloques = IntStream.range(0, 9)
				.mapToObj(b -> bloque("lunes", "%02d:00".formatted(6 + b), "%02d:30".formatted(6 + b)))
				.collect(Collectors.joining(","));

		proponer(token, "{\"asignaturas\":[{\"nombre\":\"Cálculo\",\"grupos\":[%s]}]}".formatted(trece))
				.andExpect(status().isUnprocessableEntity());
		proponer(token, "{\"asignaturas\":[{\"nombre\":\"Cálculo\",\"grupos\":[{\"codigo\":\"1\",\"bloques\":[%s]}]}]}".formatted(nueveBloques))
				.andExpect(status().isUnprocessableEntity());
	}
}
