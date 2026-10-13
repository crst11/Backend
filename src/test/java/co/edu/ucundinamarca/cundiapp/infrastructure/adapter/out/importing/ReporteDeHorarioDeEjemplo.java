package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Un Reporte de Horario inventado, con la misma distribución que el de Academusoft (SCRUM-81): las
 * mismas columnas por día y las mismas secciones, pero con nombres, documentos y salones de mentira.
 * Así ningún reporte con datos de una persona real entra al repositorio.
 *
 * <p>Sirve para dos pruebas: las palabras con su posición, que es lo que recibe el lector, y un PDF
 * armado con PDFBox, que es lo que recibe el sistema de verdad.
 */
public final class ReporteDeHorarioDeEjemplo {

	/** Un texto y dónde empieza: x desde la izquierda y arriba desde el borde superior de la página. */
	record Elemento(double x, double arriba, String texto) {
	}

	private ReporteDeHorarioDeEjemplo() {
	}

	static List<Elemento> elementos() {
		List<Elemento> e = new ArrayList<>();
		e.add(new Elemento(59, 34, "Fecha del Reporte 12-10-2026 19:03:39"));
		e.add(new Elemento(434, 34, "Académico - Academusoft 4.0"));
		e.add(new Elemento(235, 55, "Horario Creador de Oportunidades"));
		e.add(new Elemento(143, 69, "Programa"));
		e.add(new Elemento(298, 69, "Nombre del Creador de Oportunidades"));
		e.add(new Elemento(467, 69, "Número de Documento"));
		e.add(new Elemento(57, 80, "INGENIERIA DE SISTEMAS Y COMPUTACION 2020 - FUSAGASUGÁ"));
		e.add(new Elemento(340, 80, "NOMBRE DE PRUEBA APELLIDO DE PRUEBA"));
		e.add(new Elemento(483, 80, "1111111111"));
		e.add(new Elemento(94, 91, "Total de Horas"));
		e.add(new Elemento(258, 91, "Ponderación Matriculada"));
		e.add(new Elemento(443, 91, "Ubicación Semestral"));
		e.add(new Elemento(116, 102, "26"));
		e.add(new Elemento(298, 102, "17"));
		e.add(new Elemento(477, 102, "5"));

		e.add(new Elemento(261, 113, "Listado de Docentes"));
		e.add(new Elemento(122, 125, "Identificación"));
		e.add(new Elemento(344, 125, "Nombre del Gestor del Conocimiento"));
		e.add(new Elemento(39, 136, "2000000001"));
		e.add(new Elemento(261, 136, "ANA MARIA GOMEZ RUIZ"));
		e.add(new Elemento(39, 147, "2000000002"));
		e.add(new Elemento(261, 147, "BENITO PÉREZ"));
		e.add(new Elemento(39, 158, "2000000003"));
		e.add(new Elemento(261, 158, "CARLOS ALBERTO DIAZ MORA"));

		e.add(new Elemento(207, 190, "Listado de Campo Multidimensional de aprendizaje"));
		e.add(new Elemento(67, 202, "Nomenclatura"));
		e.add(new Elemento(193, 202, "Nombre del Espacio Físico"));
		e.add(new Elemento(418, 202, "Nombre Localidad"));
		e.add(new Elemento(39, 214, "A-102"));
		e.add(new Elemento(143, 214, "BLOQUE A - CENTROS DE CÓMPUTO"));
		e.add(new Elemento(340, 214, "CAMPUS UNIVERSITARIO DE FUSAGASUGA"));
		e.add(new Elemento(39, 224, "D-101"));
		e.add(new Elemento(143, 224, "BLOQUE D- ACADEMICO Y ADMINISTRATIVO"));
		e.add(new Elemento(340, 224, "CAMPUS UNIVERSITARIO DE FUSAGASUGA"));
		e.add(new Elemento(39, 235, "E-302"));
		e.add(new Elemento(143, 235, "BLOQUE E- CLAUSTRO ACADÉMICO"));
		e.add(new Elemento(340, 235, "CAMPUS UNIVERSITARIO DE FUSAGASUGA"));

		e.add(new Elemento(241, 311, "Horario - Campo de aprendizaje"));
		e.add(new Elemento(77, 323, "RE :::: El grupo no tiene asignado recurso físico"));
		e.add(new Elemento(320, 323, "DO :::: El grupo no tiene asignado Gestor del Conocimiento"));
		e.add(new Elemento(57, 334, "Campo de"));
		e.add(new Elemento(126, 338, "Lunes"));
		e.add(new Elemento(181, 338, "Martes"));
		e.add(new Elemento(242, 338, "Miércoles"));
		e.add(new Elemento(324, 338, "Jueves"));
		e.add(new Elemento(395, 338, "Viernes"));
		e.add(new Elemento(454, 338, "Sábado"));
		e.add(new Elemento(509, 338, "Domingo"));
		e.add(new Elemento(53, 342, "aprendizaje"));

		// Ecuaciones: martes y miércoles, el mismo salón, el mismo docente.
		e.add(new Elemento(51, 353, "CAD612021519"));
		e.add(new Elemento(171, 355, "09:00-10:59"));
		e.add(new Elemento(238, 355, "07:00-08:59"));
		e.add(new Elemento(55, 361, "ECUACIONES"));
		e.add(new Elemento(183, 363, "E-302"));
		e.add(new Elemento(250, 363, "E-302"));
		e.add(new Elemento(49, 369, "DIFERENCIALES"));
		e.add(new Elemento(174, 371, "2000000001"));
		e.add(new Elemento(240, 371, "2000000001"));
		e.add(new Elemento(41, 378, "Grupo"));
		e.add(new Elemento(61, 378, ":"));
		e.add(new Elemento(65, 378, "SIS.FUSA"));
		e.add(new Elemento(66, 386, "501M"));

		// Sistemas: miércoles con un salón y un docente que el reporte no lista.
		e.add(new Elemento(51, 396, "CAD612021520"));
		e.add(new Elemento(238, 398, "15:00-17:59"));
		e.add(new Elemento(315, 398, "07:00-08:59"));
		e.add(new Elemento(59, 404, "SISTEMAS"));
		e.add(new Elemento(250, 406, "Z-999"));
		e.add(new Elemento(326, 406, "A-102"));
		e.add(new Elemento(55, 413, "OPERATIVOS"));
		e.add(new Elemento(240, 414, "2999999999"));
		e.add(new Elemento(317, 414, "2000000003"));
		e.add(new Elemento(41, 421, "Grupo"));
		e.add(new Elemento(61, 421, ":"));
		e.add(new Elemento(65, 421, "SIS.FUSA"));
		e.add(new Elemento(66, 429, "501M"));

		// Software: tres días, uno por columna.
		e.add(new Elemento(51, 440, "CAD612021521"));
		e.add(new Elemento(115, 441, "07:00-08:59"));
		e.add(new Elemento(238, 441, "11:00-12:59"));
		e.add(new Elemento(387, 441, "14:00-15:59"));
		e.add(new Elemento(55, 448, "INGENIERIA"));
		e.add(new Elemento(126, 449, "A-102"));
		e.add(new Elemento(250, 449, "D-101"));
		e.add(new Elemento(398, 449, "E-302"));
		e.add(new Elemento(53, 456, "DE SOFTWARE"));
		e.add(new Elemento(116, 457, "2000000002"));
		e.add(new Elemento(240, 457, "2000000002"));
		e.add(new Elemento(389, 457, "2000000002"));
		e.add(new Elemento(55, 464, "I Grupo :"));
		e.add(new Elemento(49, 472, "SIS.FUSA"));
		e.add(new Elemento(80, 472, "501M"));

		// Lengua: una asignatura sin ninguna clase en el horario.
		e.add(new Elemento(49, 483, "CAI1002020507"));
		e.add(new Elemento(62, 491, "LENGUA"));
		e.add(new Elemento(55, 499, "EXTRANJERA"));
		e.add(new Elemento(40, 507, "III Grupo : LE3-M1"));

		e.add(new Elemento(504, 798, "Pág. 1 de 1"));
		return e;
	}

	/** Las palabras con su posición, como las entrega la lectura del PDF. */
	static List<Palabra> palabras() {
		List<Palabra> palabras = new ArrayList<>();
		for (Elemento elemento : elementos()) {
			double x = elemento.x();
			for (String texto : elemento.texto().split(" ")) {
				double ancho = texto.length() * 3.9;
				palabras.add(new Palabra(texto, x, x + ancho, elemento.arriba()));
				x += ancho + 1.8;
			}
		}
		return palabras;
	}

	public static byte[] pdf() {
		try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			PDPage pagina = new PDPage(PDRectangle.A4);
			documento.addPage(pagina);
			var fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			try (var contenido = new PDPageContentStream(documento, pagina)) {
				contenido.setFont(fuente, 6.5f);
				for (Elemento elemento : elementos()) {
					contenido.beginText();
					contenido.newLineAtOffset((float) elemento.x(), (float) (pagina.getMediaBox().getHeight() - elemento.arriba()));
					contenido.showText(elemento.texto());
					contenido.endText();
				}
			}
			documento.save(salida);
			return salida.toByteArray();
		} catch (IOException error) {
			throw new IllegalStateException(error);
		}
	}
}
