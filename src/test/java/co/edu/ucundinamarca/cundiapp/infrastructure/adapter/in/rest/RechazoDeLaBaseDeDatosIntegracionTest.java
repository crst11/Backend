package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
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

/**
 * Cuando PostgreSQL rechaza una escritura, el estudiante recibe un 422 genérico y no el 403 del token
 * CSRF (SCRUM-78).
 *
 * <p>Va contra un servidor HTTP real y no contra MockMvc, a propósito: el 403 engañoso nace del
 * reenvío interno a {@code /error} que hace el contenedor de servlets, y MockMvc no hace ese reenvío.
 * Con MockMvc la excepción simplemente se propaga y la prueba pasaría o fallaría por una razón que no
 * es la del defecto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class RechazoDeLaBaseDeDatosIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";

	@LocalServerPort
	private int puerto;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

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
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true,
						Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		var login = enviar("POST", "/api/publico/auth/login", null,
				"{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE));
		assertThat(login.statusCode()).isEqualTo(200);
		return JsonPath.read(login.body(), "$.tokenDeAcceso");
	}

	@Test
	void unaViolacionDeUnaRestriccionEsUn422GenericoSinElMensajeDePostgres() throws Exception {
		String token = entrar("rechazo.restriccion@ucundinamarca.edu.co");
		assertThat(enviar("PUT", "/api/mis/perfil-academico", token,
				"{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)).statusCode()).isEqualTo(200);

		// Más créditos aprobados que matriculados: la importación no lo valida, así que lo para
		// ck_resumen_creditos en la base de datos.
		var respuesta = enviar("POST", "/api/mis/importaciones", token, """
				{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
				  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":20,
				   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
				   "notas":[{"codigoAsignatura":"CAD612021101","nota":4.2}]}]}""");

		assertThat(respuesta.statusCode()).isEqualTo(422);
		assertThat(respuesta.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
		assertThat((String) JsonPath.read(respuesta.body(), "$.detail"))
				.isEqualTo("No pudimos guardar: los datos no cumplen una regla del modelo");
		// Nada de lo que sabe PostgreSQL viaja al cliente: tablas, columnas ni restricciones.
		assertThat(respuesta.body()).doesNotContainIgnoringCase("ck_resumen")
				.doesNotContainIgnoringCase("resumen_periodo")
				.doesNotContainIgnoringCase("violates")
				.doesNotContainIgnoringCase("constraint")
				.doesNotContainIgnoringCase("CSRF");
	}
}
