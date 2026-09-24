package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida.SesionRepositorio;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.MotivoDeRevocacion;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Sesion;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Autenticación completa con la seguridad real y PostgreSQL real: inicio de sesión, JWT, cookies,
 * CSRF, rotación del refresco, detección de reutilización, cierre de sesión y bloqueo de intentos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AutenticacionIntegracionTest {

	private static final String CLAVE = "claveSegura1";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private SesionRepositorio sesiones;

	@Autowired
	private PasswordEncoder codificador;

	private record Credenciales(String acceso, String refresco, String csrf) {
	}

	private int crearCuenta(String correo, EstadoCuenta estado) {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true, Instant.now()),
				codificador.encode(CLAVE));
		if (estado == EstadoCuenta.ACTIVA) {
			estudiantes.guardarActivacion(creada.activar());
		}
		return creada.id();
	}

	private static RequestPostProcessor desdeLaIp(String ip) {
		return peticion -> {
			peticion.setRemoteAddr(ip);
			return peticion;
		};
	}

	private MvcResult login(String correo, String contrasena, String ip) throws Exception {
		return mvc.perform(post("/api/publico/auth/login")
						.with(desdeLaIp(ip))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena)))
				.andReturn();
	}

	private Credenciales loginExitoso(String correo, String ip) throws Exception {
		MvcResult resultado = login(correo, contrasena(), ip);
		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
		return new Credenciales(
				JsonPath.read(resultado.getResponse().getContentAsString(), "$.tokenDeAcceso"),
				resultado.getResponse().getCookie("refresco").getValue(),
				resultado.getResponse().getCookie("XSRF-TOKEN").getValue());
	}

	private static String contrasena() {
		return CLAVE;
	}

	private MvcResult refrescar(Credenciales credenciales, boolean conCsrf) throws Exception {
		var peticion = post("/api/publico/auth/refresco")
				.with(desdeLaIp("10.9.9.9"))
				.cookie(new Cookie("refresco", credenciales.refresco()), new Cookie("XSRF-TOKEN", credenciales.csrf()));
		if (conCsrf) {
			peticion.header("X-XSRF-TOKEN", credenciales.csrf());
		}
		return mvc.perform(peticion).andReturn();
	}

	@Test
	void iniciaSesionConElTokenEnElCuerpoYElRefrescoSoloEnCookieHttpOnly() throws Exception {
		crearCuenta("auth.login@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);

		MvcResult resultado = login("auth.login@ucundinamarca.edu.co", CLAVE, "10.1.0.1");

		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
		String cuerpo = resultado.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(cuerpo, "$.tipo")).isEqualTo("Bearer");
		assertThat((String) JsonPath.read(cuerpo, "$.cuenta.correo")).isEqualTo("auth.login@ucundinamarca.edu.co");
		Cookie refresco = resultado.getResponse().getCookie("refresco");
		assertThat(refresco.isHttpOnly()).isTrue();
		assertThat(refresco.getPath()).isEqualTo("/api/publico/auth");
		assertThat(cuerpo).doesNotContain(refresco.getValue());
		assertThat(resultado.getResponse().getCookie("XSRF-TOKEN").isHttpOnly()).isFalse();
		assertThat(resultado.getResponse().getHeaders("Set-Cookie")).anyMatch(c -> c.contains("SameSite=Strict"));
	}

	@Test
	void elTokenDeAccesoAbreLaRutaProtegidaYSinElNoSePuedeEntrar() throws Exception {
		crearCuenta("auth.protegida@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);
		Credenciales credenciales = loginExitoso("auth.protegida@ucundinamarca.edu.co", "10.1.0.2");

		mvc.perform(get("/api/mis/cuenta").header("Authorization", "Bearer " + credenciales.acceso()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.correo").value("auth.protegida@ucundinamarca.edu.co"))
				.andExpect(jsonPath("$.estado").value("activa"));
		mvc.perform(get("/api/mis/cuenta")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/mis/cuenta").header("Authorization", "Bearer " + credenciales.acceso() + "x"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cadaEstudianteSoloVeSuPropiaCuenta() throws Exception {
		crearCuenta("auth.uno@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);
		crearCuenta("auth.dos@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);
		Credenciales uno = loginExitoso("auth.uno@ucundinamarca.edu.co", "10.1.0.3");
		Credenciales dos = loginExitoso("auth.dos@ucundinamarca.edu.co", "10.1.0.4");

		mvc.perform(get("/api/mis/cuenta").header("Authorization", "Bearer " + uno.acceso()))
				.andExpect(jsonPath("$.correo").value("auth.uno@ucundinamarca.edu.co"));
		mvc.perform(get("/api/mis/cuenta").header("Authorization", "Bearer " + dos.acceso()))
				.andExpect(jsonPath("$.correo").value("auth.dos@ucundinamarca.edu.co"));
	}

	@Test
	void unaContrasenaIncorrectaYUnCorreoInexistenteRecibenLaMismaRespuesta() throws Exception {
		crearCuenta("auth.mala@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);

		MvcResult incorrecta = login("auth.mala@ucundinamarca.edu.co", "otraClave123", "10.1.0.5");
		MvcResult inexistente = login("nadie.existe@ucundinamarca.edu.co", "otraClave123", "10.1.0.6");

		assertThat(incorrecta.getResponse().getStatus()).isEqualTo(401);
		assertThat(inexistente.getResponse().getStatus()).isEqualTo(401);
		assertThat((String) JsonPath.read(incorrecta.getResponse().getContentAsString(), "$.detail"))
				.isEqualTo(JsonPath.read(inexistente.getResponse().getContentAsString(), "$.detail"));
	}

	@Test
	void unaCuentaPendienteNoPuedeIniciarSesion() throws Exception {
		crearCuenta("auth.pendiente@ucundinamarca.edu.co", EstadoCuenta.PENDIENTE);

		MvcResult resultado = login("auth.pendiente@ucundinamarca.edu.co", CLAVE, "10.1.0.7");

		assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
		assertThat(resultado.getResponse().getContentAsString()).contains("Verifica tu correo");
	}

	@Test
	void trasCincoIntentosFallidosSeguidosBloqueaHastaConLaContrasenaCorrecta() throws Exception {
		crearCuenta("auth.bloqueo@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);
		for (int i = 0; i < 5; i++) {
			assertThat(login("auth.bloqueo@ucundinamarca.edu.co", "mala" + i + "Clave", "10.2.0." + i).getResponse().getStatus())
					.isEqualTo(401);
		}

		MvcResult resultado = login("auth.bloqueo@ucundinamarca.edu.co", CLAVE, "10.2.0.99");

		assertThat(resultado.getResponse().getStatus()).isEqualTo(429);
	}

	@Test
	void renuevaLaSesionYCambiaElRefrescoPeroExigeElTokenCsrf() throws Exception {
		crearCuenta("auth.refresco@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);
		Credenciales original = loginExitoso("auth.refresco@ucundinamarca.edu.co", "10.1.0.8");

		assertThat(refrescar(original, false).getResponse().getStatus()).isEqualTo(403);
		MvcResult renovada = refrescar(original, true);

		assertThat(renovada.getResponse().getStatus()).isEqualTo(200);
		String nuevoRefresco = renovada.getResponse().getCookie("refresco").getValue();
		assertThat(nuevoRefresco).isNotEqualTo(original.refresco());
		assertThat((String) JsonPath.read(renovada.getResponse().getContentAsString(), "$.tokenDeAcceso")).isNotBlank();
		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe(original.refresco())).orElseThrow().fueRotada()).isTrue();
	}

	@Test
	void reutilizarUnRefrescoYaRotadoCierraTodasLasSesionesDeLaCuenta() throws Exception {
		crearCuenta("auth.reuso@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);
		Credenciales original = loginExitoso("auth.reuso@ucundinamarca.edu.co", "10.1.0.9");
		MvcResult primera = refrescar(original, true);
		Credenciales rotada = new Credenciales(
				JsonPath.read(primera.getResponse().getContentAsString(), "$.tokenDeAcceso"),
				primera.getResponse().getCookie("refresco").getValue(),
				original.csrf());

		assertThat(refrescar(original, true).getResponse().getStatus()).isEqualTo(401);

		assertThat(refrescar(rotada, true).getResponse().getStatus()).isEqualTo(401);
		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe(rotada.refresco())).orElseThrow().motivoRevocacion())
				.isEqualTo(MotivoDeRevocacion.REUSO_DETECTADO);
	}

	@Test
	void cerrarSesionInvalidaElRefrescoYBorraLasCookies() throws Exception {
		crearCuenta("auth.logout@ucundinamarca.edu.co", EstadoCuenta.ACTIVA);
		Credenciales credenciales = loginExitoso("auth.logout@ucundinamarca.edu.co", "10.1.0.10");

		MvcResult cierre = mvc.perform(post("/api/publico/auth/logout")
						.cookie(new Cookie("refresco", credenciales.refresco()), new Cookie("XSRF-TOKEN", credenciales.csrf()))
						.header("X-XSRF-TOKEN", credenciales.csrf()))
				.andExpect(status().isNoContent())
				.andReturn();

		assertThat(cierre.getResponse().getCookie("refresco").getMaxAge()).isZero();
		assertThat(sesiones.buscarPorHuella(Sesion.huellaDe(credenciales.refresco())).orElseThrow().motivoRevocacion())
				.isEqualTo(MotivoDeRevocacion.CIERRE_SESION);
		assertThat(refrescar(credenciales, true).getResponse().getStatus()).isEqualTo(401);
	}

	@Test
	void renovarSinCookieDeRefrescoEsUnaSesionInvalida() throws Exception {
		mvc.perform(post("/api/publico/auth/refresco")
						.cookie(new Cookie("XSRF-TOKEN", "abc"))
						.header("X-XSRF-TOKEN", "abc"))
				.andExpect(status().isUnauthorized());
	}
}
