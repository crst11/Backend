package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * El id que lleva cada línea del registro y vuelve en {@code X-Request-Id} (12 factores, factor 11, SCRUM-213): el que
 * pone nginx de borde se respeta, el que falta se crea, y uno con saltos de línea o demasiado largo no pasa porque
 * serviría para falsificar líneas del registro.
 */
class IdDePeticionFilterTest {

	private final IdDePeticionFilter filtro = new IdDePeticionFilter();

	@AfterEach
	void limpiarElRegistro() {
		MDC.clear();
	}

	private record Resultado(String enLaRespuesta, String enElRegistroMientrasSeAtiende) {
	}

	private Resultado atender(String encabezado) throws Exception {
		MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/mis/algo");
		if (encabezado != null) {
			peticion.addHeader(IdDePeticionFilter.ENCABEZADO, encabezado);
		}
		MockHttpServletResponse respuesta = new MockHttpServletResponse();
		AtomicReference<String> dentro = new AtomicReference<>();
		MockFilterChain cadena = new MockFilterChain() {
			@Override
			public void doFilter(jakarta.servlet.ServletRequest rq, jakarta.servlet.ServletResponse rs) {
				dentro.set(MDC.get(IdDePeticionFilter.CLAVE_EN_EL_REGISTRO));
			}
		};

		filtro.doFilter(peticion, respuesta, cadena);

		return new Resultado(respuesta.getHeader(IdDePeticionFilter.ENCABEZADO), dentro.get());
	}

	@Test
	void respetaElIdQueTrajoNginxYLoDejaEnElRegistroMientrasSeAtiende() throws Exception {
		Resultado r = atender("3f9c2a7be1d84a5f9a0b6c7d8e9f0a1b");

		assertThat(r.enLaRespuesta()).isEqualTo("3f9c2a7be1d84a5f9a0b6c7d8e9f0a1b");
		assertThat(r.enElRegistroMientrasSeAtiende()).isEqualTo("3f9c2a7be1d84a5f9a0b6c7d8e9f0a1b");
	}

	@Test
	void sinIdLoCreaYEsElMismoEnLaRespuestaYEnElRegistro() throws Exception {
		Resultado r = atender(null);

		assertThat(r.enLaRespuesta()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
		assertThat(r.enElRegistroMientrasSeAtiende()).isEqualTo(r.enLaRespuesta());
	}

	@Test
	void dosPeticionesSinIdNoComparten() throws Exception {
		assertThat(atender(null).enLaRespuesta()).isNotEqualTo(atender(null).enLaRespuesta());
	}

	@Test
	void unIdConCaracteresQueFalsificarianElRegistroSeReemplaza() throws Exception {
		for (String malo : new String[] {"abc\n2026-10-18 ERROR falso", "con espacios y mas", "x<script>alert(1)</script>", "corto", "a".repeat(65), "", "   "}) {
			Resultado r = atender(malo);

			assertThat(r.enLaRespuesta()).as("«%s»", malo).isNotEqualTo(malo).matches("[0-9a-f-]{36}");
			assertThat(r.enElRegistroMientrasSeAtiende()).isEqualTo(r.enLaRespuesta());
		}
	}

	@Test
	void despuesDeAtenderLaPeticionElRegistroQuedaLimpio() throws Exception {
		atender("3f9c2a7be1d84a5f9a0b6c7d8e9f0a1b");

		assertThat(MDC.get(IdDePeticionFilter.CLAVE_EN_EL_REGISTRO)).isNull();
	}

	@Test
	void aunqueLaPeticionFalleElRegistroQuedaLimpioParaLaSiguiente() {
		MockFilterChain cadenaQueFalla = new MockFilterChain() {
			@Override
			public void doFilter(jakarta.servlet.ServletRequest rq, jakarta.servlet.ServletResponse rs) throws ServletException {
				throw new ServletException("fallo a medias");
			}
		};

		assertThatThrownBy(() -> filtro.doFilter(new MockHttpServletRequest("GET", "/api/x"), new MockHttpServletResponse(), cadenaQueFalla))
				.isInstanceOf(ServletException.class);

		assertThat(MDC.get(IdDePeticionFilter.CLAVE_EN_EL_REGISTRO)).isNull();
	}
}
