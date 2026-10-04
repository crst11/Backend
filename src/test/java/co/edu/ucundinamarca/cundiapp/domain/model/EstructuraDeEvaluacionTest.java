package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Las dos reglas del árbol de evaluación (RF05, SCRUM-27): las categorías de una asignatura suman
 * 100 % y, dentro de cada una, sus actividades también.
 *
 * <p>Se prueban aquí y no en la base de datos porque es una regla del negocio: el estudiante tiene
 * que enterarse de cuánto le falta mientras arma la estructura, no cuando el servidor estalla.
 */
class EstructuraDeEvaluacionTest {

	private static final int MATRICULA = 7;

	private static BigDecimal pct(String valor) {
		return new BigDecimal(valor);
	}

	private static ActividadEvaluativa actividad(int consecutivo, String nombre, String porcentaje) {
		return new ActividadEvaluativa(consecutivo, nombre, pct(porcentaje), null, TipoDeActividad.TALLER,
				EstadoDeEntrega.NO_ENTREGADA);
	}

	private static CategoriaDeEvaluacion categoria(int consecutivo, String nombre, String porcentaje,
			ActividadEvaluativa... actividades) {
		return new CategoriaDeEvaluacion(consecutivo, nombre, pct(porcentaje), OrigenDeCategoria.ESTUDIANTE,
				List.of(actividades));
	}

	@Nested
	@DisplayName("Arranca con la plantilla de tres cortes")
	class DesdeLaPlantilla {

		private static final PlantillaDeEvaluacion TRES_CORTES = new PlantillaDeEvaluacion(1, "Tres cortes",
				"Plantilla por defecto de la universidad", true,
				List.of(new ItemDePlantilla(1, "Primer corte", pct("30")),
						new ItemDePlantilla(2, "Segundo corte", pct("30")),
						new ItemDePlantilla(3, "Tercer corte", pct("40"))));

		@Test
		void laPlantillaSeConvierteEnTresCategoriasConSusPorcentajes() {
			var estructura = EstructuraDeEvaluacion.desdePlantilla(MATRICULA, TRES_CORTES);

			assertThat(estructura.categorias()).extracting(CategoriaDeEvaluacion::nombre)
					.containsExactly("Primer corte", "Segundo corte", "Tercer corte");
			assertThat(estructura.categorias()).extracting(CategoriaDeEvaluacion::porcentaje)
					.containsExactly(pct("30"), pct("30"), pct("40"));
		}

		@Test
		void lasCategoriasDeLaPlantillaQuedanMarcadasComoTal() {
			// Sirve para saber después qué puso el estudiante y qué venía puesto.
			var estructura = EstructuraDeEvaluacion.desdePlantilla(MATRICULA, TRES_CORTES);

			assertThat(estructura.categorias()).allMatch(c -> c.origen() == OrigenDeCategoria.PLANTILLA);
		}

		@Test
		void laPlantillaNoTraeActividades_esoLoArmaElEstudiante() {
			var estructura = EstructuraDeEvaluacion.desdePlantilla(MATRICULA, TRES_CORTES);

			assertThat(estructura.categorias()).allMatch(c -> c.actividades().isEmpty());
			assertThat(estructura.estaDefinida()).isTrue();
		}
	}

	@Nested
	@DisplayName("Las categorías suman 100 %")
	class SumaDeCategorias {

		@Test
		void treinta_treinta_cuarentaEsValido() {
			var estructura = new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Primer corte", "30"), categoria(2, "Segundo corte", "30"),
							categoria(3, "Tercer corte", "40")));

			assertThat(estructura.categorias()).hasSize(3);
		}

		@Test
		void siNoSuman100DiceCuantoFalta() {
			assertThatThrownBy(() -> new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Primer corte", "30"), categoria(2, "Segundo corte", "30"))))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("suman 60")
					.hasMessageContaining("faltan 40");
		}

		@Test
		void siSePasanDe100TambienLoDice() {
			assertThatThrownBy(() -> new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Primer corte", "60"), categoria(2, "Segundo corte", "60"))))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("suman 120")
					.hasMessageContaining("se pasan 20");
		}

		@Test
		void losDecimalesCuadranSiSuman100Exacto() {
			// Tres cortes iguales no dan 33,33 cada uno: el último carga el centésimo que sobra.
			var estructura = new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Corte 1", "33.33"), categoria(2, "Corte 2", "33.33"),
							categoria(3, "Corte 3", "33.34")));

			assertThat(estructura.categorias()).hasSize(3);
		}

		@Test
		void laSumaSeComparaPorValorNoPorFormato() {
			// 50 + 50.00 son 100, aunque "100" y "100.00" no sean iguales como texto.
			var estructura = new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Mitad", "50"), categoria(2, "Otra mitad", "50.00")));

			assertThat(estructura.categorias()).hasSize(2);
		}

		@Test
		void unaSolaCategoriaDel100TambienVale() {
			var estructura = new EstructuraDeEvaluacion(MATRICULA, List.of(categoria(1, "Examen único", "100")));

			assertThat(estructura.categorias()).hasSize(1);
		}

		@Test
		void sinCategoriasEsUnaAsignaturaQueElEstudianteNoHaConfigurado() {
			// No es un error: es el estado inicial de toda matrícula importada.
			var estructura = new EstructuraDeEvaluacion(MATRICULA, List.of());

			assertThat(estructura.estaDefinida()).isFalse();
		}
	}

	@Nested
	@DisplayName("Las actividades de cada categoría suman 100 %")
	class SumaDeActividades {

		@Test
		void unaCategoriaConSusActividadesCuadradasEsValida() {
			var estructura = new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Primer corte", "100", actividad(1, "Parcial", "70"),
							actividad(2, "Taller", "30"))));

			assertThat(estructura.categorias().getFirst().actividades()).hasSize(2);
		}

		@Test
		void unaCategoriaSinActividadesTodaviaEsValida() {
			// El estudiante define primero los cortes y después qué cae en cada uno.
			var estructura = new EstructuraDeEvaluacion(MATRICULA, List.of(categoria(1, "Primer corte", "100")));

			assertThat(estructura.categorias().getFirst().actividades()).isEmpty();
		}

		@Test
		void siLasActividadesNoSuman100DiceEnCualCategoria() {
			assertThatThrownBy(() -> categoria(1, "Segundo corte", "100", actividad(1, "Parcial", "50"),
					actividad(2, "Quiz", "30")))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("Segundo corte")
					.hasMessageContaining("suman 80")
					.hasMessageContaining("faltan 20");
		}

		@Test
		void cadaCategoriaSeRevisaPorSeparado() {
			assertThatThrownBy(() -> new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Primer corte", "50", actividad(1, "Parcial", "100")),
							categoria(2, "Segundo corte", "50", actividad(1, "Parcial", "90")))))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("Segundo corte");
		}

		@Test
		void sePuedeQuitarUnaActividadSiLasDemasVuelvenASumar100() {
			var conTres = categoria(1, "Primer corte", "100", actividad(1, "Parcial", "40"),
					actividad(2, "Taller", "30"), actividad(3, "Quiz", "30"));

			var conDos = new CategoriaDeEvaluacion(conTres.consecutivo(), conTres.nombre(), conTres.porcentaje(),
					conTres.origen(), List.of(actividad(1, "Parcial", "60"), actividad(2, "Taller", "40")));

			assertThat(conDos.actividades()).hasSize(2);
		}
	}

	@Nested
	@DisplayName("Lo que no puede entrar")
	class DatosQueNoSeAceptan {

		@Test
		void unPorcentajeEnCeroNoAportaNada() {
			assertThatThrownBy(() -> categoria(1, "Corte vacío", "0"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("mayor que 0");
		}

		@Test
		void unPorcentajeNegativoTampoco() {
			assertThatThrownBy(() -> actividad(1, "Parcial", "-10"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("mayor que 0");
		}

		@Test
		void nadieValeMasDe100PorSiSolo() {
			assertThatThrownBy(() -> categoria(1, "Corte imposible", "120"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("100");
		}

		@Test
		void dosCategoriasNoPuedenCompartirElNumeroDeOrden() {
			assertThatThrownBy(() -> new EstructuraDeEvaluacion(MATRICULA,
					List.of(categoria(1, "Primer corte", "50"), categoria(1, "Segundo corte", "50"))))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("mismo número de orden");
		}

		@Test
		void dosActividadesDeLaMismaCategoriaTampoco() {
			assertThatThrownBy(() -> categoria(1, "Primer corte", "100", actividad(1, "Parcial", "50"),
					actividad(1, "Taller", "50")))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("mismo número de orden");
		}

		@Test
		void unaCategoriaSinNombreNoLeDiceNadaAlEstudiante() {
			assertThatThrownBy(() -> categoria(1, "   ", "100"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("nombre");
		}

		@Test
		void elNombreNoPuedePasarseDeLoQueCabeEnLaBase() {
			assertThatThrownBy(() -> categoria(1, "x".repeat(81), "100"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("80");
		}

		@Test
		void elNumeroDeOrdenEmpiezaEn1() {
			assertThatThrownBy(() -> categoria(0, "Primer corte", "100"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("número de orden");
		}

		@Test
		void unPorcentajeNuloNoSePuedeSumar() {
			assertThatThrownBy(() -> new CategoriaDeEvaluacion(1, "Primer corte", null,
					OrigenDeCategoria.ESTUDIANTE, List.of()))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("porcentaje");
		}
	}

	@Nested
	@DisplayName("Lo que la pantalla necesita preguntarle")
	class ConsultasDeLaEstructura {

		private final EstructuraDeEvaluacion estructura = new EstructuraDeEvaluacion(MATRICULA,
				List.of(categoria(1, "Primer corte", "30", actividad(1, "Parcial", "100")),
						categoria(2, "Segundo corte", "30"), categoria(3, "Tercer corte", "40")));

		@Test
		void encuentraUnaCategoriaPorSuNumeroDeOrden() {
			assertThat(estructura.categoriaDe(2)).map(CategoriaDeEvaluacion::nombre).contains("Segundo corte");
		}

		@Test
		void siLaCategoriaNoExisteNoDevuelveNada() {
			assertThat(estructura.categoriaDe(9)).isEmpty();
		}

		@Test
		void diceCuantasActividadesHayEnTotal() {
			// El estudiante ve de un vistazo si ya terminó de desglosar sus cortes.
			assertThat(estructura.totalDeActividades()).isEqualTo(1);
		}

		@Test
		void unaActividadGuardaLaFechaYElTipoQueElEstudianteEligio() {
			var parcial = new ActividadEvaluativa(1, "Parcial de álgebra", pct("100"), LocalDate.of(2026, 10, 20),
					TipoDeActividad.PARCIAL, EstadoDeEntrega.NO_ENTREGADA);

			assertThat(parcial.fecha()).contains(LocalDate.of(2026, 10, 20));
			assertThat(parcial.tipo()).isEqualTo(TipoDeActividad.PARCIAL);
		}

		@Test
		void unaActividadSinFechaEsLaQueElDocenteNoHaProgramado() {
			assertThat(actividad(1, "Taller", "100").fecha()).isEmpty();
		}
	}
}
