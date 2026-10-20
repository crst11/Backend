package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisAvisos;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Una excepción que nadie previó sale como un 500 y no como un 403 con el mensaje del token CSRF (SCRUM-227).
 *
 * <p>Antes, lo que salía sin manejar del controlador hacía que el contenedor reenviara la petición a {@code /error}, Spring Security
 * la bloqueaba (todo lo no declarado es {@code denyAll}) y el cliente recibía un 403 que no tenía nada que ver con lo ocurrido. Un
 * error del servidor se disfrazaba de "no tienes permiso": quien mira los 5xx para saber si algo se rompió no veía nada, y quien
 * depura buscaba un problema de permisos que no existía (SCRUM-78 lo había dejado así a propósito).
 *
 * <p>Va contra un servidor HTTP real y no contra MockMvc por la misma razón que {@link RechazoDeLaBaseDeDatosIntegracionTest}: el
 * reenvío a {@code /error} lo hace el contenedor de servlets, y MockMvc no lo hace.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ExcepcionInesperadaIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	/** Si el detalle técnico viajara al cliente, esto delataría que se filtró. */
	private static final String SECRETO_EN_EL_MENSAJE = "contrasena-de-la-base=hunter2";

	@LocalServerPort
	private int puerto;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	@MockitoBean
	private ListarMisAvisos listarMisAvisos;

	private final HttpClient cliente = HttpClient.newHttpClient();

	private HttpResponse<String> enviar(String metodo, String ruta, String token, String cuerpo) throws Exception {
		HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
				.header("Content-Type", "application/json")
				.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
		if (token != null) {
			peticion.header("Authorization", "Bearer " + token);
		}
		return cliente.send(peticion.build(), BodyHandlers.ofString());
	}

	private String entrar(String correo) throws Exception {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true, Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		var login = enviar("POST", "/api/publico/auth/login", null,
				"{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE));
		assertThat(login.statusCode()).isEqualTo(200);
		return JsonPath.read(login.body(), "$.tokenDeAcceso");
	}

	@Test
	void unaExcepcionInesperadaEsUn500GenericoSinElDetalleTecnicoNiElMensajeDelCsrf() throws Exception {
		String token = entrar("inesperada.error@ucundinamarca.edu.co");
		when(listarMisAvisos.ejecutar(anyInt(), anyBoolean())).thenThrow(new IllegalStateException(SECRETO_EN_EL_MENSAJE));

		var respuesta = enviar("GET", "/api/mis/avisos", token, null);

		assertThat(respuesta.statusCode()).isEqualTo(500);
		assertThat(respuesta.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
		assertThat((String) JsonPath.read(respuesta.body(), "$.detail")).isEqualTo("Ocurrió un error inesperado. Intenta de nuevo.");
		assertThat(respuesta.body()).doesNotContain(SECRETO_EN_EL_MENSAJE).doesNotContain("IllegalStateException")
				.doesNotContainIgnoringCase("CSRF").doesNotContainIgnoringCase("stack");
	}

	@Test
	void sinTokenSigueSiendoUn401YNoUn500() throws Exception {
		// El manejador genérico no puede tragarse los errores de seguridad: Spring Security los traduce a 401 y 403.
		var respuesta = enviar("GET", "/api/mis/avisos", null, null);

		assertThat(respuesta.statusCode()).isEqualTo(401);
	}

	@Test
	void unCuerpoIlegibleSigueSiendoUn400ProblemDetailYNoLoCapturaElManejadorGenerico() throws Exception {
		// Los errores de MVC los atiende el manejador de Spring Boot antes que el genérico: si el genérico los capturara, un JSON
		// roto del cliente sería un 500 del servidor.
		var respuesta = enviar("POST", "/api/publico/auth/login", null, "{roto");

		assertThat(respuesta.statusCode()).isEqualTo(400);
		assertThat(respuesta.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
	}

	@Test
	void unaRutaQueNoExisteSigueSiendoUn4xxYNoUn500() throws Exception {
		String token = entrar("inesperada.ruta@ucundinamarca.edu.co");

		var respuesta = enviar("GET", "/api/mis/esto-no-existe", token, null);

		assertThat(respuesta.statusCode()).isBetween(400, 499);
	}
}
