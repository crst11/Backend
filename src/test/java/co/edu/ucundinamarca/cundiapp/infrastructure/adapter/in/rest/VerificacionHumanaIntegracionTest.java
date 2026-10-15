package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.VerificadorDeHumanoPort.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Con la verificación activada, de extremo a extremo y con la seguridad real (SCRUM-92). */
@SpringBootTest(properties = "cundiapp.turnstile.secreto=llave-de-prueba")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VerificacionHumanaIntegracionTest {

	private static final String LOGIN = "{\"correo\":\"nadie@ucundinamarca.edu.co\",\"contrasena\":\"ClaveSegura1!\"}";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private VerificadorDeHumanoPort verificador;

	@Test
	void sinPruebaElInicioDeSesionNiSiquieraSeIntenta() throws Exception {
		given(verificador.verificar(eq(null), any())).willReturn(Resultado.ROBOT);

		mvc.perform(post("/api/publico/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.title").value("Verificación pendiente"));
	}

	@Test
	void conUnaPruebaValidaLaPeticionLlegaAlInicioDeSesionYFallaPorSusCredenciales() throws Exception {
		given(verificador.verificar(eq("prueba-valida"), any())).willReturn(Resultado.HUMANO);

		mvc.perform(post("/api/publico/auth/login")
						.header(VerificacionHumanaFilter.ENCABEZADO, "prueba-valida")
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGIN))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void lasRutasQueNoSonDeEntradaNoPidenLaPrueba() throws Exception {
		mvc.perform(post("/api/publico/auth/refresco")).andExpect(status().is4xxClientError());
		org.mockito.Mockito.verifyNoInteractions(verificador);
	}
}
