package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteDeHorarioDetectado;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Leer el Reporte de Horario a partir de las palabras y su posición (SCRUM-81). Las columnas de los
 * días no existen en el texto del PDF, solo en dónde está cada palabra, por eso el lector trabaja
 * con coordenadas.
 */
class LectorDeReporteDeHorarioTest {

	private final LectorDeReporteDeHorario lector = new LectorDeReporteDeHorario();

	private ReporteDeHorarioDetectado leido() {
		return lector.leer(ReporteDeHorarioDeEjemplo.palabras());
	}

	private AsignaturaConHorario asignatura(String codigo) {
		return leido().asignaturas().stream().filter(a -> a.codigo().equals(codigo)).findFirst().orElseThrow();
	}

	@Nested
	class LoGeneral {

		@Test
		void leeElProgramaLaSedeYLaFechaDelReporte() {
			var reporte = leido();

			assertThat(reporte.programa()).isEqualTo("INGENIERIA DE SISTEMAS Y COMPUTACION");
			assertThat(reporte.sede()).isEqualTo("FUSAGASUGÁ");
			assertThat(reporte.fecha()).isEqualTo(LocalDate.of(2026, 10, 12));
			assertThat(reporte.periodo().codigo()).isEqualTo("2026-2");
		}

		@Test
		void leeTodasLasAsignaturasEnElOrdenDelReporte() {
			assertThat(leido().asignaturas()).extracting(AsignaturaConHorario::codigo)
					.containsExactly("CAD612021519", "CAD612021520", "CAD612021521", "CAI1002020507");
		}

		@Test
		void elGrupoSaleCompletoAunqueEstePartidoEnVariosRenglones() {
			assertThat(asignatura("CAD612021519").grupo()).isEqualTo("SIS.FUSA 501M");
			assertThat(asignatura("CAD612021521").grupo()).isEqualTo("SIS.FUSA 501M");
			assertThat(asignatura("CAI1002020507").grupo()).isEqualTo("LE3-M1");
		}
	}

	@Nested
	class LasClases {

		@Test
		void cadaHoraCaeEnElDiaDeSuColumna() {
			var bloques = asignatura("CAD612021519").bloques();

			assertThat(bloques).extracting(BloqueDeHorario::dia)
					.containsExactly(DiaDeLaSemana.MARTES, DiaDeLaSemana.MIERCOLES);
			assertThat(bloques.get(0).horaInicio()).isEqualTo(LocalTime.of(9, 0));
			assertThat(bloques.get(1).horaInicio()).isEqualTo(LocalTime.of(7, 0));
		}

		@Test
		void elFinSeLeeUnMinutoAntesDeLaHoraEnPuntoYSeCorrige() {
			var bloques = asignatura("CAD612021519").bloques();

			assertThat(bloques.get(0).horaFin()).isEqualTo(LocalTime.of(11, 0));
			assertThat(bloques.get(1).horaFin()).isEqualTo(LocalTime.of(9, 0));
		}

		@Test
		void unaAsignaturaConUnaClaseEnCadaColumnaLasSeparaBienYLasOrdenaPorDia() {
			var bloques = asignatura("CAD612021521").bloques();

			assertThat(bloques).extracting(BloqueDeHorario::dia)
					.containsExactly(DiaDeLaSemana.LUNES, DiaDeLaSemana.MIERCOLES, DiaDeLaSemana.VIERNES);
			assertThat(bloques).extracting(BloqueDeHorario::consecutivo).containsExactly(1, 2, 3);
		}

		@Test
		void unaAsignaturaSinClasesSeListaConSusBloquesVacios() {
			assertThat(asignatura("CAI1002020507").bloques()).isEmpty();
		}

		@Test
		void elPieDePaginaNoSeLeeComoUnaClase() {
			var total = leido().asignaturas().stream().mapToInt(a -> a.bloques().size()).sum();

			assertThat(total).isEqualTo(7);
		}
	}

	@Nested
	class ElLugar {

		@Test
		void elSalonTraeSuBloqueYSuSedeDelListadoDeEspacios() {
			var espacio = asignatura("CAD612021519").bloques().getFirst().espacio().orElseThrow();

			assertThat(espacio.salon()).isEqualTo("E-302");
			assertThat(espacio.bloque()).isEqualTo("BLOQUE E- CLAUSTRO ACADÉMICO");
			assertThat(espacio.sede()).isEqualTo("CAMPUS UNIVERSITARIO DE FUSAGASUGA");
		}

		@Test
		void elNombreLargoDelBloqueNoSeRecorta() {
			var espacio = asignatura("CAD612021521").bloques().get(1).espacio().orElseThrow();

			assertThat(espacio.bloque()).isEqualTo("BLOQUE D- ACADEMICO Y ADMINISTRATIVO");
		}

		@Test
		void unSalonQueElReporteNoListaQuedaConLaSedeDelReporte() {
			var espacio = asignatura("CAD612021520").bloques().getFirst().espacio().orElseThrow();

			assertThat(espacio.salon()).isEqualTo("Z-999");
			assertThat(espacio.bloque()).isNull();
			assertThat(espacio.sede()).isEqualTo("FUSAGASUGÁ");
		}
	}

	@Nested
	class ElDocente {

		@Test
		void elDocenteSeEnlazaPorSuDocumentoYQuedaSoloConElNombre() {
			var docente = asignatura("CAD612021519").bloques().getFirst().docente().orElseThrow();

			assertThat(docente.nombres()).isEqualTo("Ana Maria");
			assertThat(docente.apellidos()).isEqualTo("Gomez Ruiz");
		}

		@Test
		void sinDocumentoQueEnlazarNoHayDocente() {
			var bloque = asignatura("CAD612021520").bloques().stream()
					.filter(b -> b.dia() == DiaDeLaSemana.MIERCOLES).findFirst().orElseThrow();

			assertThat(bloque.docente()).isEmpty();
		}

		@Test
		void lasTildesDelNombreSeConservan() {
			var docente = asignatura("CAD612021521").bloques().getFirst().docente().orElseThrow();

			assertThat(docente.nombres()).isEqualTo("Benito");
			assertThat(docente.apellidos()).isEqualTo("Pérez");
		}

		@Test
		void ningunDocumentoDeIdentidadSobreviveALaLectura() {
			assertThat(leido().toString()).doesNotContain("2000000001", "2000000002", "2999999999", "1111111111");
		}
	}

	@Nested
	class LoQueNoEsUnReporteDeHorario {

		@Test
		void otroReporteSeRechazaDiciendoCualSeEspera() {
			var registro = List.of(
					new Palabra("Consultar", 50, 90, 40),
					new Palabra("Registro", 95, 130, 40),
					new Palabra("Académico", 135, 190, 40),
					new Palabra("Extendido", 195, 250, 40));

			assertThatThrownBy(() -> lector.leer(registro))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("Reporte de Horario");
		}

		@Test
		void sinPalabrasSeRechaza() {
			assertThatThrownBy(() -> lector.leer(List.of()))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("Reporte de Horario");
		}

		@Test
		void sinLaFechaDelReporteNoSePuedeSaberElPeriodo() {
			var sinFecha = ReporteDeHorarioDeEjemplo.palabras().stream()
					.filter(p -> p.arriba() != 34)
					.toList();

			assertThatThrownBy(() -> lector.leer(sinFecha))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("fecha");
		}
	}
}
