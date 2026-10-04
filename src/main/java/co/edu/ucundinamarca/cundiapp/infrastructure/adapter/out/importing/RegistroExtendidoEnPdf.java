package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteHistorialAcademicoPort;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteAcademicoDetectado;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Saca el texto del PDF con PDFBox y se lo pasa al lector (SCRUM-23).
 *
 * <p>Esta clase solo sabe de archivos; entender el reporte es trabajo de
 * {@link LectorDeRegistroExtendido}, que por eso se puede probar sin ningún PDF.
 *
 * <p>Un PDF es un archivo que viene de afuera: si está dañado, cifrado o no es un PDF, PDFBox
 * lanza y aquí se convierte en un mensaje que el estudiante entiende, con la salida de registrar
 * las notas a mano.
 */
@Component
class RegistroExtendidoEnPdf implements FuenteHistorialAcademicoPort {

	private final LectorDeRegistroExtendido lector;

	RegistroExtendidoEnPdf(LectorDeRegistroExtendido lector) {
		this.lector = lector;
	}

	@Override
	public ReporteAcademicoDetectado leer(byte[] archivo) {
		if (archivo == null || archivo.length == 0) {
			throw new ReglaDeNegocioVioladaException("El archivo llegó vacío");
		}
		try (PDDocument documento = Loader.loadPDF(archivo)) {
			var extractor = new PDFTextStripper();
			extractor.setSortByPosition(true);
			return lector.leer(extractor.getText(documento));
		} catch (IOException | RuntimeException error) {
			if (error instanceof ReglaDeNegocioVioladaException reglaVioladaException) {
				throw reglaVioladaException;
			}
			throw new ReglaDeNegocioVioladaException(
					"No pudimos leer el archivo. Asegúrate de subir el PDF tal como lo descargaste, sin abrirlo y volverlo a guardar");
		}
	}
}
