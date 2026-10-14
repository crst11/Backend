package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteDeHorarioPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteDeHorarioDetectado;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

/**
 * Saca del PDF las palabras con su posición y se las pasa al lector (SCRUM-81).
 *
 * <p>A diferencia del Registro Académico Extendido, aquí el texto corrido no sirve: el horario es una
 * tabla con una columna por día, y para saber de qué día es una hora hay que saber en qué columna
 * está. Esta clase solo sabe de archivos; entender el reporte es trabajo de
 * {@link LectorDeReporteDeHorario}, que por eso se prueba sin ningún PDF.
 *
 * <p>Un PDF es un archivo que viene de afuera: si está dañado, cifrado o no es un PDF, PDFBox lanza
 * y aquí se convierte en un mensaje que el estudiante entiende.
 */
@Component
class ReporteDeHorarioEnPdf implements FuenteDeHorarioPort {

	/** Cuánto se suma por página para que las palabras de varias páginas queden en una sola columna larga. */
	private static final double ALTO_RESERVADO_POR_PAGINA = 2000;

	private final LectorDeReporteDeHorario lector;

	ReporteDeHorarioEnPdf(LectorDeReporteDeHorario lector) {
		this.lector = lector;
	}

	@Override
	public ReporteDeHorarioDetectado leer(byte[] archivo) {
		if (archivo == null || archivo.length == 0) {
			throw new ReglaDeNegocioVioladaException("El archivo llegó vacío");
		}
		try (PDDocument documento = Loader.loadPDF(archivo)) {
			ValidacionDePdf.exigirSeguro(documento);
			var extractor = new ExtractorDePalabras();
			extractor.setSortByPosition(true);
			extractor.getText(documento);
			return lector.leer(extractor.palabras);
		} catch (IOException | RuntimeException error) {
			if (error instanceof ReglaDeNegocioVioladaException reglaVioladaException) {
				throw reglaVioladaException;
			}
			throw new ReglaDeNegocioVioladaException(
					"No pudimos leer el archivo. Asegúrate de subir el PDF tal como lo descargaste, sin abrirlo y volverlo a guardar");
		}
	}

	/** Recoge cada palabra con su posición mientras PDFBox recorre las páginas. */
	private static final class ExtractorDePalabras extends PDFTextStripper {

		private final List<Palabra> palabras = new ArrayList<>();

		private ExtractorDePalabras() {
			super();
		}

		/**
		 * PDFBox entrega el texto por tramos (lo que el PDF escribe de una sola vez), que pueden traer
		 * varias palabras. Se parten por los espacios para que cada palabra lleve su propia posición.
		 */
		@Override
		protected void writeString(String texto, List<TextPosition> posiciones) {
			StringBuilder palabra = new StringBuilder();
			TextPosition primera = null;
			TextPosition ultima = null;
			for (TextPosition posicion : posiciones) {
				String letra = posicion.getUnicode();
				if (letra == null || esSeparador(letra)) {
					cerrar(palabra, primera, ultima);
					palabra.setLength(0);
					primera = null;
					continue;
				}
				if (primera == null) {
					primera = posicion;
				}
				palabra.append(letra);
				ultima = posicion;
			}
			cerrar(palabra, primera, ultima);
		}

		/** El espacio sin quiebre (U+00A0) no cuenta como espacio para String.isBlank y el reporte lo usa. */
		private static boolean esSeparador(String letra) {
			return letra.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
		}

		private void cerrar(StringBuilder palabra, TextPosition primera, TextPosition ultima) {
			if (primera == null || palabra.isEmpty()) {
				return;
			}
			palabras.add(new Palabra(
					palabra.toString(),
					primera.getXDirAdj(),
					ultima.getXDirAdj() + ultima.getWidthDirAdj(),
					primera.getYDirAdj() + (getCurrentPageNo() - 1) * ALTO_RESERVADO_POR_PAGINA));
		}
	}
}
