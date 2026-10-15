package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.CorreoInstitucional;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoCuenta;
import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing.ReporteDeHorarioDeEjemplo;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Importar el Reporte de Horario de extremo a extremo, con seguridad real y PostgreSQL real
 * (SCRUM-81). El PDF se arma con PDFBox a partir de un reporte inventado con la distribución del de
 * Academusoft: así ningún reporte con datos de una persona real entra al repositorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ImportarHorarioIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";
	private static final String BASE = "/api/mis/importaciones/horario";

	/** Lo que el estudiante confirma: las cuatro asignaturas del reporte de ejemplo y una que no es de su plan. */
	private static final String CONFIRMACION = """
			{"periodo":"2026-2","asignaturas":[
			  {"codigo":"CAD612021519","grupo":"SIS.FUSA 501M","clases":[
			    {"dia":"martes","horaInicio":"09:00","horaFin":"11:00","salon":"E-302",
			     "bloque":"BLOQUE E- CLAUSTRO ACADÉMICO","sede":"CAMPUS UNIVERSITARIO DE FUSAGASUGA",
			     "docente":{"nombres":"Ana Maria","apellidos":"Gomez Ruiz"}},
			    {"dia":"miercoles","horaInicio":"07:00","horaFin":"09:00","salon":"E-302",
			     "bloque":"BLOQUE E- CLAUSTRO ACADÉMICO","sede":"CAMPUS UNIVERSITARIO DE FUSAGASUGA",
			     "docente":{"nombres":"Ana Maria","apellidos":"Gomez Ruiz"}}]},
			  {"codigo":"CAD612021521","grupo":"SIS.FUSA 501M","clases":[
			    {"dia":"lunes","horaInicio":"07:00","horaFin":"09:00","salon":"A-102",
			     "bloque":"BLOQUE A - CENTROS DE CÓMPUTO","sede":"CAMPUS UNIVERSITARIO DE FUSAGASUGA"}]},
			  {"codigo":"CAI1002020507","grupo":"LE3-M1","clases":[]},
			  {"codigo":"XXX999999999","grupo":"X.1","clases":[
			    {"dia":"lunes","horaInicio":"10:00","horaFin":"12:00"}]}]}""";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	private String entrar(String correo) throws Exception {
		var creada = estudiantes.guardarConCredencialLocal(
				new Estudiante(null, "Ana", "Díaz", new CorreoInstitucional(correo), EstadoCuenta.PENDIENTE, true,
						Instant.now()),
				codificador.encode(CLAVE));
		estudiantes.guardarActivacion(creada.activar());
		MvcResult login = mvc.perform(post("/api/publico/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.tokenDeAcceso");
	}

	private void elegirPrograma(String token) throws Exception {
		mvc.perform(put("/api/mis/perfil-academico")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"codigoPrograma\":\"%s\"}".formatted(FUSAGASUGA)))
				.andExpect(status().isOk());
	}

	private static MockMultipartFile reporte() {
		return new MockMultipartFile(
				"archivo", "horario.pdf", MediaType.APPLICATION_PDF_VALUE, ReporteDeHorarioDeEjemplo.pdf());
	}

	private void confirmar(String token, String cuerpo) throws Exception {
		mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated());
	}

	@Test
	void muestraLoDetectadoSinGuardarNada() throws Exception {
		String token = entrar("horario.analisis@ucundinamarca.edu.co");
		elegirPrograma(token);

		mvc.perform(multipart(BASE + "/analisis").file(reporte()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombreArchivo").value("horario.pdf"))
				.andExpect(jsonPath("$.periodo").value("2026-2"))
				.andExpect(jsonPath("$.coincideConMiPrograma").value(true))
				.andExpect(jsonPath("$.detectadas").value(4))
				.andExpect(jsonPath("$.clases").value(7))
				.andExpect(jsonPath("$.seVanAGuardar").value(4))
				.andExpect(jsonPath("$.asignaturas[0].codigo").value("CAD612021519"))
				.andExpect(jsonPath("$.asignaturas[0].nombre").value("Ecuaciones Diferenciales"))
				.andExpect(jsonPath("$.asignaturas[0].grupo").value("SIS.FUSA 501M"))
				.andExpect(jsonPath("$.asignaturas[0].yaEstaba").value(false))
				.andExpect(jsonPath("$.asignaturas[0].clases[0].dia").value("martes"))
				.andExpect(jsonPath("$.asignaturas[0].clases[0].horaInicio").value("09:00"))
				// El reporte dice 10:59 y la clase termina a las 11:00.
				.andExpect(jsonPath("$.asignaturas[0].clases[0].horaFin").value("11:00"))
				.andExpect(jsonPath("$.asignaturas[0].clases[0].salon").value("E-302"))
				.andExpect(jsonPath("$.asignaturas[0].clases[0].bloque").value("BLOQUE E- CLAUSTRO ACADÉMICO"))
				.andExpect(jsonPath("$.asignaturas[0].clases[0].docente.apellidos").value("Gomez Ruiz"));

		// Nada tocó el horario todavía: ese es el criterio de aceptación.
		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(0));
	}

	@Test
	void alConfirmarLasAsignaturasQuedanEnCursoYElHorarioSeVe() throws Exception {
		String token = entrar("horario.confirmar@ucundinamarca.edu.co");
		elegirPrograma(token);

		mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(CONFIRMACION))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nuevas").value(3))
				.andExpect(jsonPath("$.actualizadas").value(0))
				// La que no es de su plan se omite y se cuenta.
				.andExpect(jsonPath("$.omitidas").value(1))
				.andExpect(jsonPath("$.clases").value(3));

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(3))
				.andExpect(jsonPath("$.dias[0].dia").value("lunes"))
				.andExpect(jsonPath("$.dias[0].clases[0].codigoAsignatura").value("CAD612021521"))
				.andExpect(jsonPath("$.dias[1].dia").value("martes"))
				.andExpect(jsonPath("$.dias[1].clases[0].horaFin").value("11:00"))
				.andExpect(jsonPath("$.dias[1].clases[0].bloque").value("BLOQUE E- CLAUSTRO ACADÉMICO"))
				.andExpect(jsonPath("$.dias[1].clases[0].docente.nombres").value("Ana Maria"));
	}

	@Test
	void elBloqueConNombreLargoDelReporteCabeEnLaBaseDeDatos() throws Exception {
		String token = entrar("horario.bloquelargo@ucundinamarca.edu.co");
		elegirPrograma(token);

		confirmar(token, """
				{"periodo":"2026-2","asignaturas":[{"codigo":"CAI1002020507","grupo":"LE3-M1","clases":[
				  {"dia":"martes","horaInicio":"07:00","horaFin":"09:00","salon":"D-101",
				   "bloque":"BLOQUE D- ACADEMICO Y ADMINISTRATIVO","sede":"CAMPUS UNIVERSITARIO DE FUSAGASUGA"}]}]}""");

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.dias[0].clases[0].bloque").value("BLOQUE D- ACADEMICO Y ADMINISTRATIVO"));
	}

	@Test
	void volverAImportarReemplazaLasClasesEnVezDeDuplicarlas() throws Exception {
		String token = entrar("horario.otravez@ucundinamarca.edu.co");
		elegirPrograma(token);
		confirmar(token, CONFIRMACION);

		mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(CONFIRMACION))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nuevas").value(0))
				.andExpect(jsonPath("$.actualizadas").value(3))
				.andExpect(jsonPath("$.clases").value(3));

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(3));
	}

	@Test
	void elAnalisisDiceQueAsignaturasYaEstabanYCuantasClasesSeReemplazan() throws Exception {
		String token = entrar("horario.yaestaba@ucundinamarca.edu.co");
		elegirPrograma(token);
		confirmar(token, CONFIRMACION);

		mvc.perform(multipart(BASE + "/analisis").file(reporte()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asignaturas[0].yaEstaba").value(true))
				.andExpect(jsonPath("$.asignaturas[0].clasesQueTiene").value(2))
				.andExpect(jsonPath("$.asignaturas[1].yaEstaba").value(false));
	}

	@Test
	void lasClasesDelReporteReemplazanLasQueElEstudianteTeniaALaMano() throws Exception {
		String token = entrar("horario.reemplazo@ucundinamarca.edu.co");
		elegirPrograma(token);
		confirmar(token, CONFIRMACION);
		var idMatricula = JsonPath.<Integer>read(
				mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
						.andReturn().getResponse().getContentAsString(),
				"$.dias[0].clases[0].idMatricula");
		mvc.perform(post("/api/mis/matriculas/%d/horario".formatted(idMatricula))
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dia\":\"viernes\",\"horaInicio\":\"14:00\",\"horaFin\":\"16:00\"}"))
				.andExpect(status().isCreated());

		confirmar(token, CONFIRMACION);

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(3));
	}

	@Test
	void unaAsignaturaQueYaCerroConNotaEnEsePeriodoNoSeToca() throws Exception {
		String token = entrar("horario.cerrada@ucundinamarca.edu.co");
		elegirPrograma(token);
		mvc.perform(post("/api/mis/importaciones").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
								  {"codigo":"2026-2","creditosMatriculados":3,"creditosAprobados":3,
								   "promedioPeriodo":4.0,"promedioAcumulado":4.0,
								   "notas":[{"codigoAsignatura":"CAD612021519","nota":4.0}]}]}"""))
				.andExpect(status().isCreated());

		mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(CONFIRMACION))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nuevas").value(2))
				.andExpect(jsonPath("$.omitidas").value(2));

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(1));
	}

	@Test
	void ningunDocumentoDeIdentidadDelReporteLlegaALaRespuesta() throws Exception {
		String token = entrar("horario.privacidad@ucundinamarca.edu.co");
		elegirPrograma(token);

		String respuesta = mvc.perform(multipart(BASE + "/analisis").file(reporte())
						.header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(respuesta)
				.doesNotContain("2000000001", "2000000002", "2000000003", "2999999999", "1111111111", "NOMBRE DE PRUEBA");
	}

	@Test
	void sinProgramaElegidoNoSePuedeImportar() throws Exception {
		String token = entrar("horario.sinprograma@ucundinamarca.edu.co");

		mvc.perform(multipart(BASE + "/analisis").file(reporte()).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(containsString("elige tu programa")));
	}

	@Test
	void unArchivoQueNoEsElReporteDeHorarioSeRechazaDiciendoCualSeEspera() throws Exception {
		String token = entrar("horario.otroarchivo@ucundinamarca.edu.co");
		elegirPrograma(token);
		byte[] registro;
		try (PDDocument documento = new PDDocument(); var salida = new ByteArrayOutputStream()) {
			PDPage pagina = new PDPage();
			documento.addPage(pagina);
			try (var contenido = new PDPageContentStream(documento, pagina)) {
				contenido.beginText();
				contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
				contenido.newLineAtOffset(20, 760);
				contenido.showText("Consultar Registro Académico Extendido");
				contenido.endText();
			}
			documento.save(salida);
			registro = salida.toByteArray();
		} catch (IOException error) {
			throw new IllegalStateException(error);
		}
		var otro = new MockMultipartFile("archivo", "registro.pdf", MediaType.APPLICATION_PDF_VALUE, registro);

		mvc.perform(multipart(BASE + "/analisis").file(otro).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(containsString("Reporte de Horario")));
	}

	@Test
	void unArchivoQueNoEsPdfSeRechaza() throws Exception {
		String token = entrar("horario.notapdf@ucundinamarca.edu.co");
		elegirPrograma(token);
		var texto = new MockMultipartFile("archivo", "horario.txt", MediaType.TEXT_PLAIN_VALUE, "hola".getBytes());

		mvc.perform(multipart(BASE + "/analisis").file(texto).header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void unaConfirmacionMalFormadaSeRechazaSinGuardarNada() throws Exception {
		String token = entrar("horario.malformado@ucundinamarca.edu.co");
		elegirPrograma(token);

		mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"periodo":"2026-2","asignaturas":[{"codigo":"CAD612021519","clases":[
								  {"dia":"martes","horaInicio":"11:00","horaFin":"09:00"}]}]}"""))
				.andExpect(status().isUnprocessableEntity());

		mvc.perform(get("/api/mis/horario").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.totalDeClases").value(0));
	}

	@Test
	void sinSesionNoSePuedeImportar() throws Exception {
		mvc.perform(multipart(BASE + "/analisis").file(reporte())).andExpect(status().isUnauthorized());
		mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(CONFIRMACION))
				.andExpect(status().isUnauthorized());
	}
}
