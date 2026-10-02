package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Endurecimiento de la API (SCRUM-69): cabeceras de seguridad y límite de correos por IP. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "cundiapp.limites.correos-por-ip=3")
class EndurecimientoSeguridadIntegracionTest {

	@Autowired
	private MockMvc mvc;

	private static RequestPostProcessor desdeLaIp(String ip) {
		return peticion -> {
			peticion.setRemoteAddr(ip);
			return peticion;
		};
	}

	@Test
	void todaRespuestaTraeLasCabecerasDeSeguridad() throws Exception {
		mvc.perform(get("/api/publico/guia/categorias"))
				.andExpect(status().isOk())
				// La API solo devuelve JSON: nada de lo que responde debe ejecutarse ni incrustarse.
				.andExpect(header().string("Content-Security-Policy",
						"default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Permissions-Policy",
						"camera=(), microphone=(), geolocation=(), payment=(), usb=()"));
	}

	@Test
	void trasVariasPeticionesSeguidasLaMismaIpYaNoPuedePedirMasCorreos() throws Exception {
		String cuerpo = "{\"correo\":\"limite.correos@ucundinamarca.edu.co\"}";
		for (int i = 0; i < 3; i++) {
			mvc.perform(post("/api/publico/auth/recuperacion").with(desdeLaIp("10.9.9.9"))
							.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
					.andExpect(status().isAccepted());
		}

		mvc.perform(post("/api/publico/auth/recuperacion").with(desdeLaIp("10.9.9.9"))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Content-Type", "application/problem+json;charset=UTF-8"));
	}

	@Test
	void elLimiteEsPorIpYNoAfectaAOtroEstudiante() throws Exception {
		String cuerpo = "{\"correo\":\"limite.otraip@ucundinamarca.edu.co\"}";
		for (int i = 0; i < 4; i++) {
			mvc.perform(post("/api/publico/auth/recuperacion").with(desdeLaIp("10.9.9.8"))
					.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
		}

		// Otra IP arranca con su propio contador: bloquear una red no debe dejar fuera a los demás.
		mvc.perform(post("/api/publico/auth/recuperacion").with(desdeLaIp("10.9.9.7"))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isAccepted());
	}

	@Test
	void elLimiteNoAplicaALasRutasQueNoEnvianCorreo() throws Exception {
		// Iniciar sesión tiene su propio límite por intentos fallidos; verificar, por intentos del código.
		for (int i = 0; i < 6; i++) {
			mvc.perform(get("/api/publico/guia/categorias").with(desdeLaIp("10.9.9.6")))
					.andExpect(status().isOk());
		}
	}
}
