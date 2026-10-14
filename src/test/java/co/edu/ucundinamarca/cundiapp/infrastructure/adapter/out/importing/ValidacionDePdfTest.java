package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.junit.jupiter.api.Test;

/** Lo que se le exige a un PDF ya abierto: sin cifrar, corto y sin scripts (SCRUM-89). */
class ValidacionDePdfTest {

	private static PDDocument conPaginas(int paginas) {
		var documento = new PDDocument();
		for (int i = 0; i < paginas; i++) {
			documento.addPage(new PDPage());
		}
		return documento;
	}

	/** Se guarda y se vuelve a abrir, como llega de verdad: así el cifrado y las acciones quedan como en el archivo. */
	private static PDDocument reabrir(PDDocument documento) throws IOException {
		var salida = new ByteArrayOutputStream();
		documento.save(salida);
		documento.close();
		return Loader.loadPDF(salida.toByteArray());
	}

	@Test
	void unReporteNormalPasa() throws IOException {
		try (var documento = reabrir(conPaginas(2))) {
			assertThatCode(() -> ValidacionDePdf.exigirSeguro(documento)).doesNotThrowAnyException();
		}
	}

	@Test
	void elLimiteDePaginasEsInclusivo() throws IOException {
		try (var documento = reabrir(conPaginas(ValidacionDePdf.MAXIMO_DE_PAGINAS))) {
			assertThatCode(() -> ValidacionDePdf.exigirSeguro(documento)).doesNotThrowAnyException();
		}
	}

	@Test
	void masPaginasDeLasQueTieneUnReporteSeRechaza() throws IOException {
		try (var documento = reabrir(conPaginas(ValidacionDePdf.MAXIMO_DE_PAGINAS + 1))) {
			assertThatThrownBy(() -> ValidacionDePdf.exigirSeguro(documento))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("páginas");
		}
	}

	@Test
	void unPdfCifradoSeRechaza() throws IOException {
		var documento = conPaginas(1);
		var politica = new StandardProtectionPolicy("dueno", "", new AccessPermission());
		politica.setEncryptionKeyLength(128);
		documento.protect(politica);

		try (var cifrado = reabrir(documento)) {
			assertThatThrownBy(() -> ValidacionDePdf.exigirSeguro(cifrado))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("cifrado");
		}
	}

	@Test
	void unPdfQueEjecutaUnScriptAlAbrirseSeRechaza() throws IOException {
		var documento = conPaginas(1);
		documento.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('hola')"));

		try (var conScript = reabrir(documento)) {
			assertThatThrownBy(() -> ValidacionDePdf.exigirSeguro(conScript))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("scripts");
		}
	}
}
