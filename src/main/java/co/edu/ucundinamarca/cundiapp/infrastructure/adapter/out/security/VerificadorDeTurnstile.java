package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.security;

import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Verifica la prueba de Cloudflare Turnstile con su API `siteverify` (SCRUM-92, ADR 0007).
 *
 * <p>Lo que se le manda a Cloudflare es el resultado del widget y la IP del estudiante; nada de la
 * cuenta. Si Cloudflare no responde a tiempo, el resultado es {@code NO_DISPONIBLE} y quien llama decide:
 * no se confunde una caída de ellos con un robot.
 */
@Component
class VerificadorDeTurnstile implements VerificadorDeHumanoPort {

	private static final Logger log = LoggerFactory.getLogger(VerificadorDeTurnstile.class);
	private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(3);

	private final RestClient cliente;
	private final String secreto;

	@Autowired
	VerificadorDeTurnstile(
			@Value("${cundiapp.turnstile.secreto:}") String secreto,
			@Value("${cundiapp.turnstile.url-de-verificacion:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
					String url) {
		var fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(ESPERA_MAXIMA).build());
		fabrica.setReadTimeout(ESPERA_MAXIMA);
		this.cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
		this.secreto = secreto;
	}

	/** Para las pruebas, que le pasan un cliente atado a un servidor de mentira. */
	VerificadorDeTurnstile(RestClient cliente, String secreto) {
		this.cliente = cliente;
		this.secreto = secreto;
	}

	@Override
	public Resultado verificar(String prueba, String ip) {
		if (prueba == null || prueba.isBlank()) {
			return Resultado.ROBOT;
		}
		var formulario = new LinkedMultiValueMap<String, String>();
		formulario.add("secret", secreto);
		formulario.add("response", prueba);
		if (ip != null && !ip.isBlank()) {
			formulario.add("remoteip", ip);
		}
		try {
			Map<String, Object> respuesta = cliente.post()
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(formulario)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});
			if (respuesta == null || !(respuesta.get("success") instanceof Boolean exito)) {
				log.warn("Turnstile respondió algo que no se entiende");
				return Resultado.NO_DISPONIBLE;
			}
			return exito ? Resultado.HUMANO : Resultado.ROBOT;
		} catch (RuntimeException error) {
			// Sin la prueba, ni la IP, ni el texto de la respuesta: el log no debe servir para rastrear a nadie.
			log.warn("No se pudo consultar Turnstile: {}", error.getClass().getSimpleName());
			return Resultado.NO_DISPONIBLE;
		}
	}
}
