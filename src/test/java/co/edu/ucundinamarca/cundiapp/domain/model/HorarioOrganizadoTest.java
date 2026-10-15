package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Organizar el horario a mi gusto (RF04, SCRUM-26): filtrar por asignatura o sede, ordenar y guardar
 * la preferencia. Todo es regla de dominio: la pantalla no filtra ni ordena.
 */
class HorarioOrganizadoTest {

	private static ClaseDelHorario clase(
			int matricula, String codigo, String asignatura, DiaDeLaSemana dia, int desde, String sede) {
		var espacio = sede == null ? null : new EspacioFisico("F-1", null, sede);
		return new ClaseDelHorario(matricula, codigo, asignatura,
				new BloqueDeHorario(desde, dia, LocalTime.of(desde, 0), LocalTime.of(desde + 2, 0), espacio, null));
	}

	/** Álgebra en Fusagasugá (lunes 8 y miércoles 10), Física en Soacha (lunes 10) y Cálculo sin sede (lunes 14). */
	private static HorarioSemanal horario() {
		return new HorarioSemanal(List.of(
				clase(1, "ALG", "Álgebra Lineal", DiaDeLaSemana.LUNES, 8, "Fusagasugá"),
				clase(1, "ALG", "Álgebra Lineal", DiaDeLaSemana.MIERCOLES, 10, "Fusagasugá"),
				clase(2, "FIS", "Física I", DiaDeLaSemana.LUNES, 10, "Soacha"),
				clase(3, "CAL", "Cálculo", DiaDeLaSemana.LUNES, 14, null)));
	}

	@Nested
	class Filtrar {

		@Test
		void sinFiltroDevuelveTodoElHorario() {
			assertThat(horario().filtrar(FiltroDeHorario.SIN_FILTRO).totalDeClases()).isEqualTo(4);
		}

		@Test
		void porAsignaturaDejaSoloLasClasesDeEsaAsignatura() {
			var filtrado = horario().filtrar(new FiltroDeHorario("ALG", null));

			assertThat(filtrado.totalDeClases()).isEqualTo(2);
			assertThat(filtrado.dias()).extracting(DiaConClases::dia)
					.containsExactly(DiaDeLaSemana.LUNES, DiaDeLaSemana.MIERCOLES);
		}

		@Test
		void porSedeDejaSoloLasClasesDeEsaSede() {
			var filtrado = horario().filtrar(new FiltroDeHorario(null, "Soacha"));

			assertThat(filtrado.totalDeClases()).isEqualTo(1);
			assertThat(filtrado.delDia(DiaDeLaSemana.LUNES)).extracting(ClaseDelHorario::asignatura).containsExactly("Física I");
		}

		@Test
		void laSedeSeCompararSinImportarMayusculasNiTildes() {
			assertThat(horario().filtrar(new FiltroDeHorario(null, "FUSAGASUGA")).totalDeClases()).isEqualTo(2);
		}

		@Test
		void asignaturaYSedeJuntasSeExigenAmbas() {
			assertThat(horario().filtrar(new FiltroDeHorario("ALG", "Fusagasugá")).totalDeClases()).isEqualTo(2);
			assertThat(horario().filtrar(new FiltroDeHorario("FIS", "Fusagasugá")).totalDeClases()).isZero();
		}

		@Test
		void unaClaseSinSedeNoPasaUnFiltroDeSede() {
			// Cálculo no tiene salón ni sede: no se sabe dónde es, así que no se cuela en «Soacha».
			var filtrado = horario().filtrar(new FiltroDeHorario("CAL", "Soacha"));

			assertThat(filtrado.estaVacio()).isTrue();
		}

		@Test
		void unFiltroQueNoCoincideConNadaDevuelveUnHorarioVacioYNoFalla() {
			assertThat(horario().filtrar(new FiltroDeHorario("XXX", null)).estaVacio()).isTrue();
		}

		@Test
		void unTextoEnBlancoEsNoFiltrar() {
			assertThat(horario().filtrar(new FiltroDeHorario("  ", " ")).totalDeClases()).isEqualTo(4);
		}
	}

	@Nested
	class Ordenar {

		@Test
		void porHoraEsElOrdenPorDefectoDentroDeCadaDia() {
			assertThat(horario().delDia(DiaDeLaSemana.LUNES)).extracting(ClaseDelHorario::asignatura)
					.containsExactly("Álgebra Lineal", "Física I", "Cálculo");
			assertThat(horario().delDia(DiaDeLaSemana.LUNES, OrdenDelHorario.HORA)).extracting(ClaseDelHorario::asignatura)
					.containsExactly("Álgebra Lineal", "Física I", "Cálculo");
		}

		@Test
		void porAsignaturaAgrupaPorNombreYDespuesPorHora() {
			// Álgebra, Cálculo y Física: orden alfabético del español, no por código de carácter.
			assertThat(horario().delDia(DiaDeLaSemana.LUNES, OrdenDelHorario.ASIGNATURA))
					.extracting(ClaseDelHorario::asignatura)
					.containsExactly("Álgebra Lineal", "Cálculo", "Física I");
		}

		@Test
		void elOrdenPorAsignaturaDesempataPorHora() {
			var horario = new HorarioSemanal(List.of(
					clase(1, "ALG", "Álgebra", DiaDeLaSemana.MARTES, 14, "Fusagasugá"),
					clase(1, "ALG", "Álgebra", DiaDeLaSemana.MARTES, 8, "Fusagasugá")));

			assertThat(horario.delDia(DiaDeLaSemana.MARTES, OrdenDelHorario.ASIGNATURA))
					.extracting(c -> c.bloque().horaInicio().getHour()).containsExactly(8, 14);
		}

		@Test
		void losDiasSiguenYendoDeLunesADomingoSeaCualSeaElOrden() {
			assertThat(horario().dias(OrdenDelHorario.ASIGNATURA)).extracting(DiaConClases::dia)
					.containsExactly(DiaDeLaSemana.LUNES, DiaDeLaSemana.MIERCOLES);
		}

		@Test
		void elOrdenSeConoceComoLoGuardaLaBaseDeDatos() {
			assertThat(OrdenDelHorario.desdeBd("hora")).isEqualTo(OrdenDelHorario.HORA);
			assertThat(OrdenDelHorario.desdeBd("asignatura")).isEqualTo(OrdenDelHorario.ASIGNATURA);
			assertThatThrownBy(() -> OrdenDelHorario.desdeBd("color"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("hora");
		}
	}

	@Nested
	class LoQueSePuedeElegir {

		@Test
		void lasSedesSonLasQueTieneElHorarioSinRepetirYEnOrden() {
			assertThat(horario().sedes()).containsExactly("Fusagasugá", "Soacha");
		}

		@Test
		void unaClaseSinSedeNoAportaUnaSedeVacia() {
			var soloSinSede = new HorarioSemanal(List.of(clase(3, "CAL", "Cálculo", DiaDeLaSemana.LUNES, 14, null)));

			assertThat(soloSinSede.sedes()).isEmpty();
		}

		@Test
		void lasAsignaturasSonLasQueTieneElHorarioSinRepetirYPorNombre() {
			assertThat(horario().asignaturas()).extracting(AsignaturaDelHorario::codigo).containsExactly("ALG", "CAL", "FIS");
			assertThat(horario().asignaturas()).extracting(AsignaturaDelHorario::nombre)
					.containsExactly("Álgebra Lineal", "Cálculo", "Física I");
		}
	}

	@Nested
	class LaPreferencia {

		@Test
		void sinNadaGuardadoEmpiezaEnDiaTodasLasSedesYPorHora() {
			var porDefecto = PreferenciaDeHorario.PREDETERMINADA;

			assertThat(porDefecto.vista()).isEqualTo(VistaDelHorario.DIA);
			assertThat(porDefecto.sede()).isNull();
			assertThat(porDefecto.orden()).isEqualTo(OrdenDelHorario.HORA);
		}

		@Test
		void guardaLaVistaLaSedeYElOrden() {
			var preferencia = new PreferenciaDeHorario(VistaDelHorario.SEMANA, " Fusagasugá ", OrdenDelHorario.ASIGNATURA);

			assertThat(preferencia.vista()).isEqualTo(VistaDelHorario.SEMANA);
			assertThat(preferencia.sede()).isEqualTo("Fusagasugá");
			assertThat(preferencia.orden()).isEqualTo(OrdenDelHorario.ASIGNATURA);
		}

		@Test
		void unaSedeEnBlancoEsNoTenerSede() {
			assertThat(new PreferenciaDeHorario(VistaDelHorario.DIA, "   ", OrdenDelHorario.HORA).sede()).isNull();
		}

		@Test
		void laSedeRespetaLoQueCabeEnLaBaseDeDatos() {
			assertThatThrownBy(() -> new PreferenciaDeHorario(VistaDelHorario.DIA, "x".repeat(61), OrdenDelHorario.HORA))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("60");
		}

		@Test
		void faltaLaVistaOElOrden() {
			assertThatThrownBy(() -> new PreferenciaDeHorario(null, null, OrdenDelHorario.HORA))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("vista");
			assertThatThrownBy(() -> new PreferenciaDeHorario(VistaDelHorario.DIA, null, null))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("orden");
		}

		@Test
		void laVistaSeConoceComoLaGuardaLaBaseDeDatos() {
			assertThat(VistaDelHorario.desdeBd("dia")).isEqualTo(VistaDelHorario.DIA);
			assertThat(VistaDelHorario.desdeBd("semana")).isEqualTo(VistaDelHorario.SEMANA);
			assertThatThrownBy(() -> VistaDelHorario.desdeBd("mes"))
					.isInstanceOf(ReglaDeNegocioVioladaException.class).hasMessageContaining("dia");
		}

		@Test
		void laPreferenciaSabeConvertirseEnElFiltroYElOrdenDeLaConsulta() {
			var preferencia = new PreferenciaDeHorario(VistaDelHorario.DIA, "Soacha", OrdenDelHorario.ASIGNATURA);

			assertThat(preferencia.comoFiltro()).isEqualTo(new FiltroDeHorario(null, "Soacha"));
		}
	}
}
