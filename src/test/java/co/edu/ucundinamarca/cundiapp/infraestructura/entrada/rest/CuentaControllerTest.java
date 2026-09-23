package co.edu.ucundinamarca.cundiapp.infraestructura.entrada.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.aplicacion.puerto.entrada.RegistrarEstudiante;
import co.edu.ucundinamarca.cundiapp.dominio.excepcion.CorreoYaRegistradoException;
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
