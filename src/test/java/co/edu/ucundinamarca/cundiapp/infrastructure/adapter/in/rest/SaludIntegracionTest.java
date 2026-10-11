package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El balanceador de carga y Docker preguntan "¿estás vivo?" sin sesión (SCRUM-70).
 *
 * <p>Se abre solo lo necesario: liveness y readiness. Cualquier otra ruta de Actuator sigue
 * cerrada, porque /actuator/env o /actuator/beans le entregarían a cualquiera el mapa del servidor.
 *
 * <p>Se usa readiness y no la salud agregada a propósito: la agregada incluye la base de datos, y un
 * parpadeo de Supabase haría que el balanceador diera de baja todas las tareas a la vez en vez de
 * una caída de unos segundos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SaludIntegracionTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void readinessResponde200SinSesion() throws Exception {
		mvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("UP")));
	}

	@Test
	void livenessResponde200SinSesion() throws Exception {
		mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
	}

	@Test
	void laSaludAgregadaNoSeAbreAlPublico() throws Exception {
		// Incluye la base de datos: no debe ser lo que decida si una tarea vive o muere.
		mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
	}

	@Test
	void elRestoDeActuatorSigueCerrado() throws Exception {
		mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
		mvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
		mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
	}
}
