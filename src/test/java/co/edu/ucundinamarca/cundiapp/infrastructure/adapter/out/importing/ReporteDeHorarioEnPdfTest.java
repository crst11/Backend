package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

/** Leer un PDF de verdad de punta a punta, con un Reporte de Horario inventado (SCRUM-81). */
class ReporteDeHorarioEnPdfTest {

	private final ReporteDeHorarioEnPdf fuente = new ReporteDeHorarioEnPdf(new LectorDeReporteDeHorario());

	@Test
	void leeUnPdfArmadoConLaDistribucionDelReporteDeAcademusoft() {
		var reporte = fuente.leer(ReporteDeHorarioDeEjemplo.pdf());

		assertThat(reporte.programa()).isEqualTo("INGENIERIA DE SISTEMAS Y COMPUTACION");
		assertThat(reporte.periodo().codigo()).isEqualTo("2026-2");
		assertThat(reporte.asignaturas()).extracting(AsignaturaConHorario::codigo)
				.containsExactly("CAD612021519", "CAD612021520", "CAD612021521", "CAI1002020507");

		var ecuaciones = reporte.asignaturas().getFirst();
		assertThat(ecuaciones.grupo()).isEqualTo("SIS.FUSA 501M");
		assertThat(ecuaciones.bloques()).extracting(b -> b.dia())
				.containsExactly(DiaDeLaSemana.MARTES, DiaDeLaSemana.MIERCOLES);
		assertThat(ecuaciones.bloques().getFirst().horaFin().toString()).isEqualTo("11:00");
		assertThat(ecuaciones.bloques().getFirst().espacio().orElseThrow().salon()).isEqualTo("E-302");
		assertThat(ecuaciones.bloques().getFirst().docente().orElseThrow().apellidos()).isEqualTo("Gomez Ruiz");
		assertThat(reporte.asignaturas().stream().mapToInt(a -> a.bloques().size()).sum()).isEqualTo(7);
	}

	@Test
	void unPdfQueNoEsElReporteDeHorarioSeRechazaDiciendoCualSeEspera() throws IOException {
		byte[] registro;
		try (var documento = new PDDocument(); var salida = new ByteArrayOutputStream()) {
			var pagina = new PDPage();
			documento.addPage(pagina);
			try (var contenido = new PDPageContentStream(documento, pagina)) {
				contenido.beginText();
				contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
				contenido.newLineAtOffset(50, 700);
				contenido.showText("Consultar Registro Académico Extendido");
				contenido.endText();
			}
			documento.save(salida);
			registro = salida.toByteArray();
		}

		assertThatThrownBy(() -> fuente.leer(registro))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("Reporte de Horario");
	}

	@Test
	void unArchivoQueNoEsUnPdfSeRechazaConUnMensajeQueSeEntiende() {
		assertThatThrownBy(() -> fuente.leer("esto no es un pdf".getBytes()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("No pudimos leer el archivo");
	}

	@Test
	void unArchivoVacioSeRechaza() {
		assertThatThrownBy(() -> fuente.leer(new byte[0]))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("vacío");
	}
}
