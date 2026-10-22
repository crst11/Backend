package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Los bordes exactos de las validaciones del dominio (SCRUM-242, pruebas de mutacion).
 *
 * <p>PIT cambia {@code >} por {@code >=} y quita las llamadas a las reglas; las pruebas por ejemplo de cada modelo no lo notaban
 * porque ninguna probaba justo el valor limite (el nombre de 80 caracteres, los 72 bytes de bcrypt, la nota 5.0). Aqui cada
 * limite se prueba por los dos lados: lo ultimo que se acepta y lo primero que se rechaza.
 */
class LimitesDelModeloTest {

	private static void rechaza(Executable construir) {
		assertThatThrownBy(construir::execute).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	private static void acepta(Executable construir) {
		assertThatCode(construir::execute).doesNotThrowAnyException();
	}

	private static BigDecimal pct(String valor) {
		return new BigDecimal(valor);
	}

	@Nested
	class Evaluacion {

		private ActividadEvaluativa actividad(int consecutivo, String nombre, String porcentaje) {
			return new ActividadEvaluativa(consecutivo, nombre, pct(porcentaje), null, TipoDeActividad.PARCIAL,
					EstadoDeEntrega.SIN_CALIFICAR);
		}

		@Test
		void laActividadExigeConsecutivoNombreYPorcentajeValidos() {
			rechaza(() -> actividad(0, "Parcial", "50"));
			acepta(() -> actividad(1, "Parcial", "50"));
			rechaza(() -> actividad(1, " ", "50"));
			rechaza(() -> actividad(1, "Parcial", "0"));
			rechaza(() -> actividad(1, "Parcial", "100.01"));
			acepta(() -> actividad(1, "Parcial", "100"));
		}

		@Test
		void elNombreDeLaActividadAdmiteHasta120Caracteres() {
			acepta(() -> actividad(1, "a".repeat(120), "50"));
			rechaza(() -> actividad(1, "a".repeat(121), "50"));
		}

		@Test
		void elItemDeLaPlantillaExigeConsecutivoNombreYPorcentajeValidos() {
			rechaza(() -> new ItemDePlantilla(0, "Corte", pct("30")));
			acepta(() -> new ItemDePlantilla(1, "Corte", pct("30")));
			rechaza(() -> new ItemDePlantilla(1, "", pct("30")));
			rechaza(() -> new ItemDePlantilla(1, "Corte", pct("0")));
			rechaza(() -> new ItemDePlantilla(1, "Corte", null));
		}

		@Test
		void elNombreDelItemAdmiteHasta80Caracteres() {
			acepta(() -> new ItemDePlantilla(1, "a".repeat(80), pct("30")));
			rechaza(() -> new ItemDePlantilla(1, "a".repeat(81), pct("30")));
		}

		private PlantillaDeEvaluacion plantilla(String nombre, List<ItemDePlantilla> items) {
			return new PlantillaDeEvaluacion(1, nombre, null, false, items);
		}

		private List<ItemDePlantilla> treintaTreintaCuarenta() {
			return List.of(new ItemDePlantilla(1, "Primero", pct("30")), new ItemDePlantilla(2, "Segundo", pct("30")),
					new ItemDePlantilla(3, "Tercero", pct("40")));
		}

		@Test
		void laPlantillaExigeNombreItemsConsecutivosDistintosYPesosQueSumen100() {
			acepta(() -> plantilla("30/30/40", treintaTreintaCuarenta()));
			rechaza(() -> plantilla(" ", treintaTreintaCuarenta()));
			rechaza(() -> plantilla("30/30/40", null));
			rechaza(() -> plantilla("30/30/40", List.of()));
			rechaza(() -> plantilla("repetidos", List.of(new ItemDePlantilla(1, "A", pct("50")),
					new ItemDePlantilla(1, "B", pct("50")))));
			rechaza(() -> plantilla("no suma", List.of(new ItemDePlantilla(1, "A", pct("50")),
					new ItemDePlantilla(2, "B", pct("40")))));
		}

		@Test
		void elNombreDeLaPlantillaAdmiteHasta80Caracteres() {
			acepta(() -> plantilla("a".repeat(80), treintaTreintaCuarenta()));
			rechaza(() -> plantilla("a".repeat(81), treintaTreintaCuarenta()));
		}

		@Test
		void laEstructuraVaSobreUnaMatriculaConIdentificador() {
			rechaza(() -> new EstructuraDeEvaluacion(0, List.of()));
			acepta(() -> new EstructuraDeEvaluacion(1, List.of()));
		}

		@Test
		void elMensajeDiceCuantoFaltaOCuantoSePasa() {
			assertThatThrownBy(() -> plantilla("corta", List.of(new ItemDePlantilla(1, "A", pct("60")),
					new ItemDePlantilla(2, "B", pct("30")))))
					.hasMessageContaining("suman 90 %").hasMessageContaining("faltan 10");
			assertThatThrownBy(() -> new EstructuraDeEvaluacion(1, List.of(
					new CategoriaDeEvaluacion(1, "A", pct("70"), OrigenDeCategoria.ESTUDIANTE, List.of()),
					new CategoriaDeEvaluacion(2, "B", pct("50"), OrigenDeCategoria.ESTUDIANTE, List.of()))))
					.hasMessageContaining("suman 120 %").hasMessageContaining("se pasan 20");
		}
	}

	@Nested
	class Matricula {

		@Test
		void laMatriculaNecesitaIdentificador() {
			rechaza(() -> new AsignaturaMatriculada(0, "ING101", "Calculo", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 0, 0));
			acepta(() -> new AsignaturaMatriculada(1, "ING101", "Calculo", 3, "2026-2", EstadoDeAsignatura.EN_CURSO, 0, 0));
		}

		@Test
		void laNotaDefinitivaVaDeCeroACincoAmbosIncluidos() {
			acepta(() -> new AsignaturaCursada("ING101", "Calculo", 3, pct("0"), EstadoDeAsignatura.REPROBADA));
			acepta(() -> new AsignaturaCursada("ING101", "Calculo", 3, pct("5.0"), EstadoDeAsignatura.APROBADA));
			rechaza(() -> new AsignaturaCursada("ING101", "Calculo", 3, pct("-0.01"), EstadoDeAsignatura.REPROBADA));
			rechaza(() -> new AsignaturaCursada("ING101", "Calculo", 3, pct("5.01"), EstadoDeAsignatura.APROBADA));
		}

		@Test
		void ponderaEnElPromedioSoloLoQueTieneCreditosNotaYNoEstaCancelado() {
			assertThat(new AsignaturaCursada("A", "A", 3, pct("4.0"), EstadoDeAsignatura.APROBADA).ponderaEnElPromedio()).isTrue();
			assertThat(new AsignaturaCursada("A", "A", 0, pct("4.0"), EstadoDeAsignatura.APROBADA).ponderaEnElPromedio()).isFalse();
			assertThat(new AsignaturaCursada("A", "A", 3, null, EstadoDeAsignatura.EN_CURSO).ponderaEnElPromedio()).isFalse();
			assertThat(new AsignaturaCursada("A", "A", 3, pct("4.0"), EstadoDeAsignatura.CANCELADA).ponderaEnElPromedio()).isFalse();
		}

		@Test
		void laMetaAdmiteHastaDosDecimalesYNoPasaDeCinco() {
			acepta(() -> new Meta(pct("5")));
			acepta(() -> new Meta(pct("3.45")));
			rechaza(() -> new Meta(pct("3.456")));
			rechaza(() -> new Meta(pct("5.01")));
			rechaza(() -> new Meta(pct("-0.1")));
		}

		@Test
		void elProgramaExigeCodigoCreditosYPeriodos() {
			acepta(() -> new ProgramaAcademico("ING-SIS", "Ingenieria de Sistemas", "Ingenieria", "Fusagasuga", 1, 1));
			rechaza(() -> new ProgramaAcademico(null, "Ingenieria", "Ingenieria", "Fusagasuga", 160, 10));
			rechaza(() -> new ProgramaAcademico(" ", "Ingenieria", "Ingenieria", "Fusagasuga", 160, 10));
			rechaza(() -> new ProgramaAcademico("ING-SIS", "Ingenieria", "Ingenieria", "Fusagasuga", 0, 10));
			rechaza(() -> new ProgramaAcademico("ING-SIS", "Ingenieria", "Ingenieria", "Fusagasuga", 160, 0));
		}

		@Test
		void elResumenOficialDistingueLoQueLaUniversidadNoReporto() {
			var vacio = new ResumenOficialDePeriodo("2026-1", 18, 15, null, null);
			assertThat(vacio.promedioDelPeriodo()).isEmpty();
			assertThat(vacio.acumulado()).isEmpty();

			var completo = new ResumenOficialDePeriodo("2026-1", 18, 15, pct("4.1"), pct("3.9"));
			assertThat(completo.promedioDelPeriodo()).contains(pct("4.1"));
			assertThat(completo.acumulado()).contains(pct("3.9"));
		}
	}

	@Nested
	class Cuenta {

		private final CorreoInstitucional correo = new CorreoInstitucional("ana.perez@ucundinamarca.edu.co");

		@Test
		void soloLaCuentaPendienteEstaPendiente() {
			var activa = new Estudiante(1, "Ana", "Diaz", correo, EstadoCuenta.ACTIVA, true, Instant.now());
			assertThat(activa.estaPendiente()).isFalse();
			assertThat(activa.estaActiva()).isTrue();
		}

		@Test
		void laContrasenaAdmiteHasta72BytesPorqueBcryptNoLeeMas() {
			acepta(() -> new Contrasena("Aa1!" + "x".repeat(68)));
			rechaza(() -> new Contrasena("Aa1!" + "x".repeat(69)));
			// Los acentos pesan dos bytes: 4 + 34 * 2 son justo 72 y un acento mas se pasa.
			acepta(() -> new Contrasena("Aa1!" + "é".repeat(34)));
			rechaza(() -> new Contrasena("Aa1!" + "é".repeat(35)));
		}

		@Test
		void laLongitudMinimaEsDeOchoCaracteres() {
			assertThat(Contrasena.requisitosQueFaltan("Abcdef1!")).isEmpty();
			assertThat(Contrasena.requisitosQueFaltan("Abcde1!")).containsExactly("al menos 8 caracteres");
		}

		@Test
		void laContrasenaNuevaNoPuedeContenerElUsuarioDelCorreoSiTieneCuatroOMasLetras() {
			var corto = new CorreoInstitucional("ana@ucundinamarca.edu.co");
			var justo = new CorreoInstitucional("juan@ucundinamarca.edu.co");

			// Con tres letras el usuario es demasiado comun para prohibirlo; con cuatro ya se prohibe.
			assertThat(Contrasena.nueva("Ana12345!", corto).valor()).isEqualTo("Ana12345!");
			rechaza(() -> Contrasena.nueva("Juan1234!", justo));
			rechaza(() -> Contrasena.nueva("xxJUAN123!", justo));
			assertThat(Contrasena.nueva("Otra1234!", correo)).isEqualTo(new Contrasena("Otra1234!"));
		}

		@Test
		void laContrasenaNuncaSeEscribe() {
			assertThat(new Contrasena("Secreta123!").toString()).isEqualTo("Contrasena[oculta]").doesNotContain("Secreta");
		}
	}

	@Nested
	class Calculos {

		@Test
		void unNivelEmpeoraSoloSiElNuevoEsMasGraveQueElActual() {
			assertThat(NivelDeRiesgo.BAJO.empeoraAlPasarA(NivelDeRiesgo.MEDIO)).isTrue();
			assertThat(NivelDeRiesgo.MEDIO.empeoraAlPasarA(NivelDeRiesgo.ALTO)).isTrue();
			assertThat(NivelDeRiesgo.MEDIO.empeoraAlPasarA(NivelDeRiesgo.MEDIO)).isFalse();
			assertThat(NivelDeRiesgo.ALTO.empeoraAlPasarA(NivelDeRiesgo.MEDIO)).isFalse();
		}

		@Test
		void losMinutosDeClaseSumanLasTresFranjas() {
			var resumen = new PropuestaDeHorario.Resumen(60, 120, 30, 15, List.of(), List.of(), List.of());
			assertThat(resumen.minutosDeClase()).isEqualTo(210);
		}

		@Test
		void elBloqueSeMideEnMinutosDesdeLaMedianoche() {
			var bloque = new BloqueDeOferta(DiaDeLaSemana.LUNES, LocalTime.of(9, 30), LocalTime.of(10, 45));
			assertThat(bloque.inicioEnMinutos()).isEqualTo(570);
			assertThat(bloque.finEnMinutos()).isEqualTo(645);
			assertThat(bloque.minutos()).isEqualTo(75);
		}
	}
}
