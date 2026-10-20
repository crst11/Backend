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
import java.util.List;
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
 * reenvío interno que el contenedor de servlets hace a la ruta de error, y MockMvc no hace ese
 * reenvío. Con MockMvc la excepción simplemente se propaga y la prueba pasaría o fallaría por una
 * razón que no es la del defecto.
 *
 * <p>La provocación es un <em>trigger</em> y no una restricción CHECK, que es el síntoma con el que
 * se reportó el defecto. Antes se usaba un período con más créditos aprobados que matriculados, pero
 * esa regla pasó al dominio en SCRUM-79 y ahora se rechaza antes de llegar a la base de datos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class RechazoDeLaBaseDeDatosIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String ALGEBRA = "CAD612021101";

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

	/** Elige programa e importa un período válido, que es la única forma de tener una matrícula hoy. */
	private int conUnaMatricula(String token) throws Exception {
		assertThat(enviar("PUT", "/api/mis/perfil-academico", token,
				"{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)).statusCode()).isEqualTo(200);
		assertThat(enviar("POST", "/api/mis/importaciones", token, """
				{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
				  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":3,
				   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
				   "notas":[{"codigoAsignatura":"%s","nota":4.2}]}]}""".formatted(ALGEBRA))
				.statusCode()).isEqualTo(201);

		var lista = enviar("GET", "/api/mis/matriculas", token, null);
		assertThat(lista.statusCode()).isEqualTo(200);
		List<Integer> ids = JsonPath.read(lista.body(),
				"$[?(@.codigoAsignatura == '%s')].idMatricula".formatted(ALGEBRA));
		return ids.getFirst();
	}

	@Test
	void unRechazoDelTriggerEsUn422GenericoSinElMensajeDePostgres() throws Exception {
		String token = entrar("rechazo.trigger@ucundinamarca.edu.co");
		int matricula = conUnaMatricula(token);

		// 33.333 + 33.333 + 33.334 suman 100 exacto en BigDecimal, así que el dominio los acepta. Pero la
		// columna es NUMERIC(5,2): PostgreSQL redondea cada uno a 33.33, el trigger diferido
		// trg_suma_categorias ve 99.99 y rechaza la escritura al confirmar la transacción.
		var respuesta = enviar("PUT", "/api/mis/matriculas/" + matricula + "/evaluacion", token, """
				{"categorias":[
				  {"consecutivo":1,"nombre":"Primer corte","porcentaje":33.333,"actividades":[]},
				  {"consecutivo":2,"nombre":"Segundo corte","porcentaje":33.333,"actividades":[]},
				  {"consecutivo":3,"nombre":"Final","porcentaje":33.334,"actividades":[]}]}""");

		assertThat(respuesta.statusCode()).isEqualTo(422);
		assertThat(respuesta.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
		assertThat((String) JsonPath.read(respuesta.body(), "$.detail"))
				.isEqualTo("No pudimos guardar: los datos no cumplen una regla del modelo");
		// Nada de lo que sabe PostgreSQL viaja al cliente: tablas, columnas, restricciones ni triggers.
		assertThat(respuesta.body()).doesNotContainIgnoringCase("trg_suma")
				.doesNotContainIgnoringCase("categoria_evaluacion")
				.doesNotContainIgnoringCase("porcentajes de las categor")
				.doesNotContainIgnoringCase("constraint")
				.doesNotContainIgnoringCase("CSRF");
	}
}
