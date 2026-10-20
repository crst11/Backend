package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort.Resultado;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** La prueba de persona se exige solo donde y cuando está activada (SCRUM-92). */
class VerificacionHumanaFilterTest {

	private final List<String> preguntas = new ArrayList<>();

	private VerificadorDeHumanoPort verificador(Resultado resultado) {
		return (prueba, ip) -> {
			preguntas.add(prueba + "@" + ip);
			return resultado;
		};
	}

	private static MockHttpServletRequest peticion(String metodo, String ruta, String prueba) {
		var peticion = new MockHttpServletRequest(metodo, ruta);
		peticion.setRemoteAddr("203.0.113.9");
		if (prueba != null) {
			peticion.addHeader(VerificacionHumanaFilter.ENCABEZADO, prueba);
		}
		return peticion;
	}

	private static MockHttpServletResponse filtrar(
			VerificacionHumanaFilter filtro, MockHttpServletRequest peticion, MockFilterChain cadena) throws Exception {
		var respuesta = new MockHttpServletResponse();
		filtro.doFilter(peticion, respuesta, cadena);
		return respuesta;
	}

	@Test
	void apagadoNoPreguntaNadaYDejaPasar() throws Exception {
		var filtro = new VerificacionHumanaFilter(verificador(Resultado.ROBOT), "", 0);
		var cadena = new MockFilterChain();

		var respuesta = filtrar(filtro, peticion("POST", "/api/publico/auth/login", null), cadena);

		assertThat(respuesta.getStatus()).isEqualTo(200);
		assertThat(cadena.getRequest()).isNotNull();
		assertThat(preguntas).isEmpty();
	}

	@Test
	void unaPruebaValidaDejaPasarYSeLeMandaLaIpDeLaConexion() throws Exception {
		var filtro = new VerificacionHumanaFilter(verificador(Resultado.HUMANO), "secreto", 0);
		var cadena = new MockFilterChain();

		filtrar(filtro, peticion("POST", "/api/publico/auth/registro", "prueba-1"), cadena);

		assertThat(cadena.getRequest()).isNotNull();
		assertThat(preguntas).containsExactly("prueba-1@203.0.113.9");
	}

	@Test
	void sinPruebaOConPruebaRechazadaResponde422YNoSigue() throws Exception {
		var filtro = new VerificacionHumanaFilter(verificador(Resultado.ROBOT), "secreto", 0);
		var cadena = new MockFilterChain();

		var respuesta = filtrar(filtro, peticion("POST", "/api/publico/auth/recuperacion", null), cadena);

		assertThat(respuesta.getStatus()).isEqualTo(422);
		assertThat(respuesta.getContentType()).startsWith("application/problem+json");
		assertThat(respuesta.getContentAsString()).contains("confirmar que eres una persona");
		assertThat(cadena.getRequest()).isNull();
	}

	@Test
	void siNoSePuedeConsultarAlTerceroElLoginSeDejaPasar() throws Exception {
		var filtro = new VerificacionHumanaFilter(verificador(Resultado.NO_DISPONIBLE), "secreto", 0);
		var cadena = new MockFilterChain();

		var respuesta = filtrar(filtro, peticion("POST", "/api/publico/auth/login", "prueba"), cadena);

		assertThat(respuesta.getStatus()).isEqualTo(200);
		assertThat(cadena.getRequest()).isNotNull();
	}

	@Test
	void siNoSePuedeConsultarAlTerceroElRegistroYLaRecuperacionNoSeguenYResponden503() throws Exception {
		var filtro = new VerificacionHumanaFilter(verificador(Resultado.NO_DISPONIBLE), "secreto", 0);

		for (var ruta : List.of("/api/publico/auth/registro", "/api/publico/auth/recuperacion")) {
			var cadena = new MockFilterChain();

			var respuesta = filtrar(filtro, peticion("POST", ruta, "prueba"), cadena);

			assertThat(respuesta.getStatus()).as(ruta).isEqualTo(503);
			assertThat(respuesta.getContentType()).as(ruta).startsWith("application/problem+json");
			assertThat(respuesta.getContentAsString()).as(ruta).contains("\"status\":503").contains("en unos minutos");
			assertThat(cadena.getRequest()).as(ruta).isNull();
		}
	}

	@Test
	void soloProtegeLasTresRutasYSoloPorPost() throws Exception {
		var filtro = new VerificacionHumanaFilter(verificador(Resultado.ROBOT), "secreto", 0);

		for (var libre : List.of(
				peticion("GET", "/api/publico/auth/login", null),
				peticion("POST", "/api/publico/auth/refresco", null),
				peticion("POST", "/api/publico/auth/verificacion", null),
				peticion("POST", "/api/mis/importaciones/analisis", null))) {
			var cadena = new MockFilterChain();
			filtrar(filtro, libre, cadena);
			assertThat(cadena.getRequest()).as(libre.getMethod() + " " + libre.getRequestURI()).isNotNull();
		}
		assertThat(preguntas).isEmpty();
	}

	@Test
	void detrasDeProxyLaIpEsLaQueAgregoElProxyYNoLaQueEscribioElCliente() throws Exception {
		var filtro = new VerificacionHumanaFilter(verificador(Resultado.HUMANO), "secreto", 1);
		var peticion = peticion("POST", "/api/publico/auth/login", "p");
		peticion.addHeader("X-Forwarded-For", "falsa, 198.51.100.4");

		filtrar(filtro, peticion, new MockFilterChain());

		assertThat(preguntas).containsExactly("p@198.51.100.4");
	}
}
