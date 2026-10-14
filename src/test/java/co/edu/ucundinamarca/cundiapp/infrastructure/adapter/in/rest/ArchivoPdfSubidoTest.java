package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** Un PDF que llega de afuera se revisa por su contenido y no solo por su nombre (SCRUM-89). */
class ArchivoPdfSubidoTest {

	private static MockMultipartFile archivo(String nombre, byte[] contenido) {
		return new MockMultipartFile("archivo", nombre, MediaType.APPLICATION_PDF_VALUE, contenido);
	}

	private static byte[] bytes(String texto) {
		return texto.getBytes(StandardCharsets.US_ASCII);
	}

	@Test
	void unPdfDeVerdadSeDevuelveTalCual() {
		byte[] pdf = bytes("%PDF-1.4\nresto del archivo");

		assertThat(ArchivoPdfSubido.leer(archivo("reporte.pdf", pdf), "Reporte de Horario")).isEqualTo(pdf);
	}

	@Test
	void unTxtRenombradoAPdfSeRechazaPorSuContenido() {
		var falso = archivo("reporte.pdf", bytes("esto es solo texto"));

		assertThatThrownBy(() -> ArchivoPdfSubido.leer(falso, "Reporte de Horario"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessage("El reporte tiene que ser el archivo PDF que descargaste");
	}

	@Test
	void unArchivoMuyCortoParaTenerLaFirmaSeRechaza() {
		assertThatThrownBy(() -> ArchivoPdfSubido.leer(archivo("reporte.pdf", bytes("%PD")), "Reporte de Horario"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void sinArchivoOVacioDiceQueFalta() {
		assertThatThrownBy(() -> ArchivoPdfSubido.leer(null, "Reporte de Horario")).hasMessage("Falta el archivo");
		assertThatThrownBy(() -> ArchivoPdfSubido.leer(archivo("reporte.pdf", new byte[0]), "Reporte de Horario"))
				.hasMessage("Falta el archivo");
	}

	@Test
	void masDeCincoMegasSeRechazaYDiceDeQueReporteSeTrata() {
		byte[] grande = new byte[(int) ArchivoPdfSubido.TAMANO_MAXIMO + 1];
		System.arraycopy(bytes("%PDF-"), 0, grande, 0, 5);

		assertThatThrownBy(() -> ArchivoPdfSubido.leer(archivo("reporte.pdf", grande), "Registro Académico Extendido"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("5 MB")
				.hasMessageContaining("Registro Académico Extendido");
	}

	@Test
	void unNombreQueNoTerminaEnPdfSeRechazaAunqueElContenidoSeaPdf() {
		assertThatThrownBy(() -> ArchivoPdfSubido.leer(archivo("reporte.txt", bytes("%PDF-1.4")), "Reporte de Horario"))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}
}
