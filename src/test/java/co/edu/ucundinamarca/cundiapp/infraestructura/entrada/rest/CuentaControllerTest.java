package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.ReenviarCodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.VerificarCorreo;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.CorreoYaRegistradoException;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CuentaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CuentaControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrarEstudiante registrarEstudiante;

	@MockitoBean
	private VerificarCorreo verificarCorreo;

	@MockitoBean
	private ReenviarCodigoDeVerificacion reenviarCodigo;

	@Test
	void verificaElCorreoYRespondeLaCuentaActiva() throws Exception {
		var correo = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");
		given(verificarCorreo.ejecutar("ana.diaz@ucundinamarca.edu.co", "123456")).willReturn(
				new Estudiante(1, "Ana", "Díaz", correo, EstadoCuenta.ACTIVA, true, Instant.now()));

		mvc.perform(post("/api/publico/auth/verificacion")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"ana.diaz@ucundinamarca.edu.co","codigo":"123456"}"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"id":1,"correo":"ana.diaz@ucundinamarca.edu.co","estado":"activa"}"""));
	}

	@Test
	void respondeUnprocessableSiElCodigoEsIncorrecto() throws Exception {
		given(verificarCorreo.ejecutar(any(), any()))
				.willThrow(new ReglaDeNegocioVioladaException("El código es incorrecto. Te quedan 4 intentos"));

		mvc.perform(post("/api/publico/auth/verificacion")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"ana.diaz@ucundinamarca.edu.co","codigo":"000000"}"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().json("""
						{"detail":"El código es incorrecto. Te quedan 4 intentos"}"""));
	}

	@Test
	void rechazaUnCodigoQueNoTieneSeisDigitos() throws Exception {
		mvc.perform(post("/api/publico/auth/verificacion")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"ana.diaz@ucundinamarca.edu.co","codigo":"12ab"}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void elReenvioResponde202() throws Exception {
		mvc.perform(post("/api/publico/auth/verificacion/reenvio")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"ana.diaz@ucundinamarca.edu.co"}"""))
				.andExpect(status().isAccepted());

		verify(reenviarCodigo).ejecutar("ana.diaz@ucundinamarca.edu.co");
	}

	@Test
	void registraLaCuentaYRespondeCreado() throws Exception {
		var correo = new CorreoInstitucional("ana.diaz@ucundinamarca.edu.co");
		given(registrarEstudiante.ejecutar(any())).willReturn(
				new Estudiante(1, "Ana", "Díaz", correo, EstadoCuenta.PENDIENTE, true, Instant.now()));

		mvc.perform(post("/api/publico/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"ana.diaz@ucundinamarca.edu.co","contrasena":"unaClaveSegura",
								 "nombres":"Ana","apellidos":"Díaz","aceptaTratamientoDatos":true}"""))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"id":1,"correo":"ana.diaz@ucundinamarca.edu.co","estado":"pendiente"}"""));
	}

	@Test
	void respondeConflictoSiElCorreoYaTieneCuenta() throws Exception {
		given(registrarEstudiante.ejecutar(any()))
				.willThrow(new CorreoYaRegistradoException("Ya existe una cuenta con ese correo institucional"));

		mvc.perform(post("/api/publico/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"ana.diaz@ucundinamarca.edu.co","contrasena":"unaClaveSegura",
								 "nombres":"Ana","apellidos":"Díaz","aceptaTratamientoDatos":true}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void respondeErrorDeValidacionSiFaltanDatos() throws Exception {
		mvc.perform(post("/api/publico/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"correo":"","contrasena":"corta","nombres":"","apellidos":"","aceptaTratamientoDatos":false}"""))
				.andExpect(status().isBadRequest());
	}
}
