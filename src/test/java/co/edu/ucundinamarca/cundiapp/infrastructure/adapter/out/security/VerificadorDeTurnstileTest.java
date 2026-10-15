package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Lo que se le manda a Cloudflare y cómo se entiende lo que contesta (SCRUM-92). */
class VerificadorDeTurnstileTest {

	private static final String URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

	private final RestClient.Builder constructor = RestClient.builder().baseUrl(URL);
	private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
	private final VerificadorDeTurnstile verificador = new VerificadorDeTurnstile(constructor.build(), "llave-secreta");

	@Test
	void mandaLaLlaveLaPruebaYLaIpComoFormularioYAceptaLoQueCloudflareAcepta() {
		servidor.expect(requestTo(URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().string("secret=llave-secreta&response=prueba-1&remoteip=203.0.113.9"))
				.andRespond(withSuccess("{\"success\":true,\"challenge_ts\":\"2026-10-14T10:00:00Z\"}", MediaType.APPLICATION_JSON));

		assertThat(verificador.verificar("prueba-1", "203.0.113.9")).isEqualTo(Resultado.HUMANO);
		servidor.verify();
	}

	@Test
	void unaPruebaRechazadaEsUnRobot() {
		servidor.expect(requestTo(URL))
				.andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}", MediaType.APPLICATION_JSON));

		assertThat(verificador.verificar("mala", "203.0.113.9")).isEqualTo(Resultado.ROBOT);
	}

	@Test
	void sinPruebaEsUnRobotSinPreguntarle() {
		assertThat(verificador.verificar(null, "203.0.113.9")).isEqualTo(Resultado.ROBOT);
		assertThat(verificador.verificar("  ", "203.0.113.9")).isEqualTo(Resultado.ROBOT);
		servidor.verify();
	}

	@Test
	void unaCaidaDeCloudflareNoSeConfundeConUnRobot() {
		servidor.expect(requestTo(URL)).andRespond(withServerError());

		assertThat(verificador.verificar("prueba", "203.0.113.9")).isEqualTo(Resultado.NO_DISPONIBLE);
	}

	@Test
	void unaRespuestaQueNoSeEntiendeTampocoEsUnRobot() {
		servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"algo\":\"raro\"}", MediaType.APPLICATION_JSON));

		assertThat(verificador.verificar("prueba", "203.0.113.9")).isEqualTo(Resultado.NO_DISPONIBLE);
	}

	@Test
	void sinIpNoMandaElCampoRemoteip() {
		servidor.expect(requestTo(URL))
				.andExpect(content().string("secret=llave-secreta&response=prueba"))
				.andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

		assertThat(verificador.verificar("prueba", null)).isEqualTo(Resultado.HUMANO);
		servidor.verify();
	}
}
