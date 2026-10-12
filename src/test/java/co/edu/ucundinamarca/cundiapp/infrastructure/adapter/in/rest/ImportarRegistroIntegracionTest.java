package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

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
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
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
 * Importar el Registro Académico Extendido de extremo a extremo, con seguridad real y PostgreSQL
 * real (SCRUM-23).
 *
 * <p>El PDF se arma aquí mismo con PDFBox a partir de un reporte de ejemplo: así la prueba no
 * depende de ningún archivo y, sobre todo, ningún reporte con datos de una persona real entra al
 * repositorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ImportarRegistroIntegracionTest {

	private static final String CLAVE = "ClaveSegura1!";
	private static final String FUSAGASUGA = "109964";

	/** Primer período del plan de Fusagasugá, con el formato del reporte real. */
	private static final List<String> REPORTE = List.of(
			"Fecha del Reporte 07-09-2026 16:38:48 Academico - Academusoft 4.0",
			"Consultar Registro Académico Extendido",
			"Información",
			"Identificación Nombre",
			"1111111111 NOMBRE DE PRUEBA APELLIDO DE PRUEBA",
			"Categoría Situación Total de Créditos Cursados Total de Créditos Aprobados Promedio Acumulado",
			"ANTIGUO ACTIVO 16 16 4.4",
			"Programa Jornada Ruta de Aprendizaje Ubicación Semestral",
			"INGENIERIA DE SISTEMAS Y COMPUTACION 2020 - MIXTA INGENIERIA DE SISTEMAS Y 5",
			"FUSAGASUGÁ COMPUTACION 2020 FUSAGASUGA",
			"Período Período Ponderación Ponderación Aprobada Promedio Período Promedio Acumulado",
			"Institucional Matriculada",
			"2024 - 2 16 16 4.4 4.4",
			"Código Campo de aprendizaje Tipo Ponderación Grupo Final Hab. Def.",
			"CAD612021101 ALGEBRA LINEAL NORMAL 3 F.103M 4,2 4,2",
			"CAD612021106 FUNDAMENTOS DE ELECTRONICA NORMAL 4 F.104M 4,3 4,3",
			"CAD612021103 FUNDAMENTOS DE INGENIERIA NORMAL 2 F.102M 4,3 4,3",
			"CAD612021105 MATEMATICAS DISCRETAS NORMAL 2 F.102M 4,6 4,6",
			"CAD612021102 PENSAMIENTO ALGORITMICO NORMAL 3 F.102M 4,7 4,7",
			"CAD612021104 PENSAMIENTO SISTEMICO Y AUTOMATIZACION NORMAL 2 F.101M 4,8 4,8",
			"DN-CAI1002020201 DIAGNOSTICO Y NIVELATORIO RAZONAMIENTO NORMAL 0 DN-RLC 5,0 5,0",
			"XXX999999999 ASIGNATURA QUE NO ESTA EN EL PLAN NORMAL 3 X.1 4,0 4,0",
			"Pág 1 de 1.");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EstudianteRepositorio estudiantes;

	@Autowired
	private PasswordEncoder codificador;

	private static byte[] comoPdf(List<String> lineas) throws IOException {
		try (PDDocument documento = new PDDocument(); var salida = new ByteArrayOutputStream()) {
			PDPage pagina = new PDPage();
			documento.addPage(pagina);
			var fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			try (var contenido = new PDPageContentStream(documento, pagina)) {
				contenido.beginText();
				contenido.setFont(fuente, 8);
				contenido.setLeading(10);
				contenido.newLineAtOffset(20, 760);
				for (String linea : lineas) {
					contenido.showText(linea);
					contenido.newLine();
				}
				contenido.endText();
			}
			documento.save(salida);
			return salida.toByteArray();
		}
	}

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

	private MockMultipartFile reporte() throws IOException {
		return new MockMultipartFile(
				"archivo", "registro_extendido.pdf", MediaType.APPLICATION_PDF_VALUE, comoPdf(REPORTE));
	}

	private String analizar(String token) throws Exception {
		return mvc.perform(multipart("/api/mis/importaciones/analisis")
						.file(reporte())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void muestraLoDetectadoSinGuardarNada() throws Exception {
		String correo = "importar.analisis@ucundinamarca.edu.co";
		String token = entrar(correo);
		elegirPrograma(token);

		mvc.perform(multipart("/api/mis/importaciones/analisis")
						.file(reporte())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodos.length()").value(1))
				.andExpect(jsonPath("$.periodos[0].codigo").value("2024-2"))
				.andExpect(jsonPath("$.detectadas").value(8))
				// La que no está en el plan se muestra pero no se va a guardar.
				.andExpect(jsonPath("$.seVanAGuardar").value(7))
				.andExpect(jsonPath("$.coincideConMiPrograma").value(true));

		// Nada tocó el historial todavía: ese es el criterio de aceptación.
		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.periodos.length()").value(0));
	}

	@Test
	void leDiceCualesNoEstanEnSuRutaDeAprendizaje() throws Exception {
		String token = entrar("importar.fueradelplan@ucundinamarca.edu.co");
		elegirPrograma(token);

		String analisis = analizar(token);
		List<Boolean> enElPlan = JsonPath.read(analisis, "$.periodos[0].asignaturas[*].enElPlan");

		assertThat(enElPlan).contains(false);
		List<String> fueraDelPlan =
				JsonPath.read(analisis, "$.periodos[0].asignaturas[?(@.enElPlan == false)].codigo");
		org.assertj.core.api.Assertions.assertThat(fueraDelPlan).containsExactly("XXX999999999");
	}

	@Test
	void alConfirmarQuedaGuardadoYSeVeEnElHistorial() throws Exception {
		String token = entrar("importar.confirmar@ucundinamarca.edu.co");
		elegirPrograma(token);
		analizar(token);

		mvc.perform(post("/api/mis/importaciones")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombreArchivo":"registro_extendido.pdf","detectadas":8,"periodos":[
								  {"codigo":"2024-2","creditosMatriculados":16,"creditosAprobados":16,
								   "promedioPeriodo":4.4,"promedioAcumulado":4.4,"notas":[
								    {"codigoAsignatura":"CAD612021101","nota":4.2},
								    {"codigoAsignatura":"CAD612021106","nota":4.3},
								    {"codigoAsignatura":"CAD612021103","nota":4.3},
								    {"codigoAsignatura":"CAD612021105","nota":4.6},
								    {"codigoAsignatura":"CAD612021102","nota":4.7},
								    {"codigoAsignatura":"CAD612021104","nota":4.8},
								    {"codigoAsignatura":"DN-CAI1002020201","nota":5.0},
								    {"codigoAsignatura":"XXX999999999","nota":4.0}]}]}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.guardadas").value(7))
				.andExpect(jsonPath("$.actualizadas").value(0))
				.andExpect(jsonPath("$.omitidas").value(1));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.periodos.length()").value(1))
				.andExpect(jsonPath("$.periodos[0].creditosMatriculados").value(16))
				// El promedio oficial que trajo el reporte manda sobre el que calcula la app.
				.andExpect(jsonPath("$.periodos[0].promedio").value(4.4))
				.andExpect(jsonPath("$.periodos[0].fuenteDelPromedio").value("oficial"));
	}

	@Test
	void volverAImportarActualizaEnVezDeDuplicar() throws Exception {
		String token = entrar("importar.otravez@ucundinamarca.edu.co");
		elegirPrograma(token);
		String cuerpo = """
				{"nombreArchivo":"registro_extendido.pdf","detectadas":1,"periodos":[
				  {"codigo":"2024-2","creditosMatriculados":3,"creditosAprobados":3,
				   "promedioPeriodo":4.2,"promedioAcumulado":4.2,
				   "notas":[{"codigoAsignatura":"CAD612021101","nota":4.2}]}]}""";

		mvc.perform(post("/api/mis/importaciones").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(jsonPath("$.guardadas").value(1));
		mvc.perform(post("/api/mis/importaciones").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(jsonPath("$.guardadas").value(0))
				.andExpect(jsonPath("$.actualizadas").value(1));

		mvc.perform(get("/api/mis/historial").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.periodos[0].asignaturas.length()").value(1));
	}

	@Test
	void sinProgramaElegidoNoSePuedeImportar() throws Exception {
		String token = entrar("importar.sinprograma@ucundinamarca.edu.co");

		mvc.perform(multipart("/api/mis/importaciones/analisis")
						.file(reporte())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("elige tu programa")));
	}

	@Test
	void unArchivoQueNoEsElReporteSeRechazaConUnMensajeUtil() throws Exception {
		String token = entrar("importar.otroarchivo@ucundinamarca.edu.co");
		elegirPrograma(token);
		var otro = new MockMultipartFile("archivo", "notas.pdf", MediaType.APPLICATION_PDF_VALUE,
				comoPdf(List.of("Consultar Notas Actuales", "CAD612021523 BASE DE DATOS 2 - 0.0")));

		mvc.perform(multipart("/api/mis/importaciones/analisis").file(otro)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail")
						.value(org.hamcrest.Matchers.containsString("Registro Académico Extendido")));
	}

	@Test
	void unArchivoQueNoEsPdfSeRechaza() throws Exception {
		String token = entrar("importar.notapdf@ucundinamarca.edu.co");
		elegirPrograma(token);
		var texto = new MockMultipartFile("archivo", "notas.txt", MediaType.TEXT_PLAIN_VALUE, "hola".getBytes());

		mvc.perform(multipart("/api/mis/importaciones/analisis").file(texto)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void sinSesionNoSePuedeImportar() throws Exception {
		mvc.perform(multipart("/api/mis/importaciones/analisis").file(reporte()))
				.andExpect(status().isUnauthorized());
	}

	private static org.assertj.core.api.ListAssert<Boolean> assertThat(List<Boolean> valores) {
		return org.assertj.core.api.Assertions.assertThat(valores);
	}
}
