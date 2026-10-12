package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Con un balanceador delante, el límite por IP cuenta al estudiante y no al balanceador (SCRUM-71).
 *
 * <p>Esto es lo que se rompía: todas las peticiones llegaban desde la IP del balanceador, así que
 * quien agotaba el cupo de correos dejaba sin cupo a todos los demás.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {"cundiapp.limites.correos-por-ip=2", "cundiapp.red.saltos-de-proxy=1"})
class LimitePorIpDetrasDeProxyIntegracionTest {

	private static final String BALANCEADOR = "10.0.3.25";
	private static final String CUERPO = "{\"correo\":\"proxy.limite@ucundinamarca.edu.co\"}";

	@Autowired
	private MockMvc mvc;

	private ResultActions pedirCorreoDesde(String clienteReal, String falsa)
			throws Exception {
		String reenviado = falsa == null ? clienteReal : falsa + ", " + clienteReal;
		return mvc.perform(post("/api/publico/auth/recuperacion")
				.with(peticion -> {
					peticion.setRemoteAddr(BALANCEADOR);
					return peticion;
				})
				.header("X-Forwarded-For", reenviado)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CUERPO));
	}

	@Test
	void unEstudianteQueAgotaSuCupoNoDejaSinCupoAOtroQueEntraPorElMismoBalanceador() throws Exception {
		pedirCorreoDesde("190.24.10.7", null).andExpect(status().isAccepted());
		pedirCorreoDesde("190.24.10.7", null).andExpect(status().isAccepted());
		pedirCorreoDesde("190.24.10.7", null).andExpect(status().isTooManyRequests());

		// Otro estudiante, mismo balanceador: tiene su propio cupo.
		pedirCorreoDesde("181.55.2.14", null).andExpect(status().isAccepted());
	}

	@Test
	void ponerseUnaIpFalsaNoReiniciaElCupo() throws Exception {
		pedirCorreoDesde("190.77.1.1", "1.1.1.1").andExpect(status().isAccepted());
		pedirCorreoDesde("190.77.1.1", "2.2.2.2").andExpect(status().isAccepted());

		// El cliente cambia la IP que escribe en cada petición, pero el balanceador agrega la verdadera.
		pedirCorreoDesde("190.77.1.1", "3.3.3.3").andExpect(status().isTooManyRequests());
	}
}
