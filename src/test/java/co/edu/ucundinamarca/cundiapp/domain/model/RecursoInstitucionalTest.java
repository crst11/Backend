package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** La regla de búsqueda de la guía: se prueba sin base de datos ni HTTP. */
class RecursoInstitucionalTest {

	private static final CategoriaDeRecurso TRAMITES = new CategoriaDeRecurso(2, "Trámites y calendario");
	private static final CategoriaDeRecurso PLANTILLAS = new CategoriaDeRecurso(3, "Plantillas y formatos");

	private static RecursoInstitucional recurso(CategoriaDeRecurso categoria, String titulo, String descripcion, String url) {
		return new RecursoInstitucional(
				1, categoria, titulo, descripcion, url, TipoDeRecurso.ENLACE, EstadoDeRecurso.VIGENTE, LocalDate.of(2026, 9, 26));
	}

	private final RecursoInstitucional cancelaciones = recurso(TRAMITES, "Adición y cancelación de materias",
			"Cómo adicionar o cancelar materias, y cómo cancelar el semestre.", "https://www.ucundinamarca.edu.co/index.php/estudiantes");
	private final RecursoInstitucional plantillaWord = recurso(PLANTILLAS, "Plantilla de documento en Word",
			"Plantilla oficial de Word para informes.", "https://www.ucundinamarca.edu.co/sgc/documents/plantillas/PLANTILLA-FORMATO-WORD.docx");

	@Test
	void buscaSinDistinguirMayusculasNiTildes() {
		assertThat(cancelaciones.coincideCon("ADICION")).isTrue();
		assertThat(cancelaciones.coincideCon("cancelación")).isTrue();
		assertThat(cancelaciones.coincideCon("Cómo Cancelar")).isTrue();
	}

	@Test
	void exigeTodasLasPalabrasEnCualquierOrden() {
		assertThat(cancelaciones.coincideCon("materias cancelar")).isTrue();
		assertThat(cancelaciones.coincideCon("cancelar grados")).isFalse();
	}

	@Test
	void aceptaSingularOPluralComoLoEscribeUnaPersona() {
		assertThat(plantillaWord.coincideCon("plantilla")).isTrue();
		assertThat(plantillaWord.coincideCon("plantillas")).isTrue();
		assertThat(plantillaWord.coincideCon("informe")).isTrue();
		assertThat(plantillaWord.coincideCon("documentos")).isTrue();
	}

	@Test
	void tambienBuscaEnElNombreDeLaCategoria() {
		assertThat(cancelaciones.coincideCon("tramites")).isTrue();
		assertThat(plantillaWord.coincideCon("formatos")).isTrue();
	}

	@Test
	void unaBusquedaVaciaOSoloConSignosCoincideConTodo() {
		assertThat(cancelaciones.coincideCon(null)).isTrue();
		assertThat(cancelaciones.coincideCon("   ")).isTrue();
		assertThat(cancelaciones.coincideCon("¿?")).isTrue();
	}

	@Test
	void diceElFormatoDelArchivoOVacioSiEsUnaPagina() {
		assertThat(plantillaWord.formatoDeArchivo()).contains("Word");
		assertThat(recurso(TRAMITES, "Calendario", null, "https://x.edu.co/CALENDARIO.PDF").formatoDeArchivo()).contains("PDF");
		assertThat(recurso(PLANTILLAS, "Excel", null, "https://x.edu.co/a.xlsx").formatoDeArchivo()).contains("Excel");
		assertThat(recurso(PLANTILLAS, "Diapositivas", null, "https://x.edu.co/a.pptx").formatoDeArchivo()).contains("PowerPoint");
		assertThat(cancelaciones.formatoDeArchivo()).isEmpty();
	}

	@Test
	void soloEstaVigenteSiSuEstadoLoDice() {
		assertThat(cancelaciones.estaVigente()).isTrue();
		var enRevision = new RecursoInstitucional(1, TRAMITES, "X", null, "https://x.edu.co", TipoDeRecurso.ENLACE,
				EstadoDeRecurso.PENDIENTE_REVISION, null);
		assertThat(enRevision.estaVigente()).isFalse();
	}

	@Test
	void exigeCategoriaTituloTipoEstadoYEnlaceSeguroASuFuente() {
		assertThatThrownBy(() -> recurso(null, "X", null, "https://x.edu.co")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> recurso(TRAMITES, " ", null, "https://x.edu.co")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> recurso(TRAMITES, "X", null, "http://x.edu.co")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new RecursoInstitucional(1, TRAMITES, "X", null, "https://x.edu.co", null, EstadoDeRecurso.VIGENTE, null))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void losValoresDeLaBaseDeDatosSeConviertenDeIdaYVuelta() {
		for (TipoDeRecurso tipo : TipoDeRecurso.values()) {
			assertThat(TipoDeRecurso.desdeBd(tipo.valorEnBd())).isEqualTo(tipo);
		}
		for (EstadoDeRecurso estado : EstadoDeRecurso.values()) {
			assertThat(EstadoDeRecurso.desdeBd(estado.valorEnBd())).isEqualTo(estado);
		}
		assertThatThrownBy(() -> TipoDeRecurso.desdeBd("video")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EstadoDeRecurso.desdeBd("borrado")).isInstanceOf(IllegalArgumentException.class);
	}
}
