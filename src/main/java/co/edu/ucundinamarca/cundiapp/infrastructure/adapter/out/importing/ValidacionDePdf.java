package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;

/**
 * Lo que se le exige a un PDF una vez abierto, antes de leerle el texto (SCRUM-89). Los reportes de la
 * universidad son cortos, sin contraseña y sin scripts: lo que no cumple eso no es el reporte, y leerlo
 * solo gasta recursos del servidor. Lo usan los dos lectores de PDF.
 */
final class ValidacionDePdf {

	/** Los reportes de la universidad tienen una o dos páginas; con holgura. */
	static final int MAXIMO_DE_PAGINAS = 20;

	private ValidacionDePdf() {
	}

	static void exigirSeguro(PDDocument documento) {
		if (documento.isEncrypted()) {
			throw new ReglaDeNegocioVioladaException(
					"El PDF está protegido o cifrado. Sube el reporte tal como lo descargaste, sin protegerlo");
		}
		if (documento.getNumberOfPages() > MAXIMO_DE_PAGINAS) {
			throw new ReglaDeNegocioVioladaException(
					"El PDF tiene más de %d páginas y el reporte es mucho más corto. Revisa que sea el correcto"
							.formatted(MAXIMO_DE_PAGINAS));
		}
		if (traeContenidoActivo(documento.getDocumentCatalog())) {
			throw new ReglaDeNegocioVioladaException(
					"El PDF trae contenido activo (scripts) y no se puede leer. Sube el reporte tal como lo descargaste");
		}
	}

	private static boolean traeContenidoActivo(PDDocumentCatalog catalogo) {
		var nombres = catalogo.getNames();
		if (nombres != null && nombres.getJavaScript() != null) {
			return true;
		}
		try {
			return catalogo.getOpenAction() instanceof PDAction;
		} catch (IOException error) {
			// Una acción de apertura ilegible tampoco es de un reporte normal.
			return true;
		}
	}
}
