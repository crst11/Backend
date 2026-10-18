package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Lo que el estudiante escribe de la oferta de matrícula: bloques, grupos y asignaturas (RF13, SCRUM-207). */
class OfertaDeHorarioTest {

	private static BloqueDeOferta bloque(DiaDeLaSemana dia, String inicio, String fin) {
		return new BloqueDeOferta(dia, LocalTime.parse(inicio), LocalTime.parse(fin));
	}

	private static GrupoOfertado grupo(String codigo, BloqueDeOferta... bloques) {
		return new GrupoOfertado(codigo, List.of(bloques));
	}

	// --- Bloque ----------------------------------------------------------------------------------------------

	@Test
	void unBloqueTieneQueEmpezarAntesDeTerminar() {
		assertThatThrownBy(() -> bloque(DiaDeLaSemana.LUNES, "10:00", "08:00")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> bloque(DiaDeLaSemana.LUNES, "08:00", "08:00")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new BloqueDeOferta(null, LocalTime.of(8, 0), LocalTime.of(10, 0)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new BloqueDeOferta(DiaDeLaSemana.LUNES, null, LocalTime.of(10, 0)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void unBloqueSeGuardaAlMinuto() {
		BloqueDeOferta b = new BloqueDeOferta(DiaDeLaSemana.LUNES, LocalTime.of(8, 0, 30), LocalTime.of(10, 0, 45));

		assertThat(b.inicio()).isEqualTo(LocalTime.of(8, 0));
		assertThat(b.fin()).isEqualTo(LocalTime.of(10, 0));
		assertThat(b.minutos()).isEqualTo(120);
	}

	@Test
	void dosBloquesChocanSoloSiSonDelMismoDiaYSeSolapan() {
		BloqueDeOferta lunes8a10 = bloque(DiaDeLaSemana.LUNES, "08:00", "10:00");

		assertThat(lunes8a10.chocaCon(bloque(DiaDeLaSemana.LUNES, "09:00", "11:00"))).isTrue();
		assertThat(lunes8a10.chocaCon(bloque(DiaDeLaSemana.LUNES, "08:30", "09:00"))).isTrue();
		assertThat(lunes8a10.chocaCon(bloque(DiaDeLaSemana.LUNES, "07:00", "12:00"))).isTrue();
		assertThat(lunes8a10.chocaCon(bloque(DiaDeLaSemana.MARTES, "08:00", "10:00"))).isFalse();
		assertThat(lunes8a10.chocaCon(bloque(DiaDeLaSemana.LUNES, "13:00", "15:00"))).isFalse();
	}

	@Test
	void terminarALaHoraEnQueEmpiezaOtraClaseNoEsChoque() {
		BloqueDeOferta lunes8a10 = bloque(DiaDeLaSemana.LUNES, "08:00", "10:00");

		assertThat(lunes8a10.chocaCon(bloque(DiaDeLaSemana.LUNES, "10:00", "12:00"))).isFalse();
		assertThat(bloque(DiaDeLaSemana.LUNES, "06:00", "08:00").chocaCon(lunes8a10)).isFalse();
	}

	// --- Franja ----------------------------------------------------------------------------------------------

	@Test
	void lasFranjasPartenElDiaEnMananaTardeYNoche() {
		assertThat(FranjaDelDia.MANANA.minutosQueCaenEn(7 * 60, 9 * 60)).isEqualTo(120);
		assertThat(FranjaDelDia.TARDE.minutosQueCaenEn(7 * 60, 9 * 60)).isZero();
		// Una clase de 11 a 13 reparte una hora en la mañana y otra en la tarde.
		assertThat(FranjaDelDia.MANANA.minutosQueCaenEn(11 * 60, 13 * 60)).isEqualTo(60);
		assertThat(FranjaDelDia.TARDE.minutosQueCaenEn(11 * 60, 13 * 60)).isEqualTo(60);
		assertThat(FranjaDelDia.NOCHE.minutosQueCaenEn(17 * 60, 20 * 60)).isEqualTo(120);
	}

	@Test
	void laFranjaSeLeeDeLoQueMandaLaPantallaYUnValorMaloEsUnaReglaDeNegocio() {
		assertThat(FranjaDelDia.desdeApi("manana")).isEqualTo(FranjaDelDia.MANANA);
		assertThat(FranjaDelDia.desdeApi(" Tarde ")).isEqualTo(FranjaDelDia.TARDE);
		assertThatThrownBy(() -> FranjaDelDia.desdeApi("madrugada")).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	// --- Grupo -----------------------------------------------------------------------------------------------

	@Test
	void unGrupoNecesitaCodigoYAlMenosUnBloque() {
		BloqueDeOferta b = bloque(DiaDeLaSemana.LUNES, "08:00", "10:00");

		assertThatThrownBy(() -> new GrupoOfertado(" ", List.of(b))).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new GrupoOfertado("1", List.of())).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new GrupoOfertado("x".repeat(21), List.of(b))).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThat(new GrupoOfertado(" 1 ", List.of(b)).codigo()).isEqualTo("1");
	}

	@Test
	void unGrupoNoAdmiteMasDeOchoBloques() {
		List<BloqueDeOferta> nueve = IntStream.range(0, 9)
				.mapToObj(i -> bloque(DiaDeLaSemana.values()[i % 6], i < 6 ? "08:00" : "14:00", i < 6 ? "10:00" : "16:00"))
				.toList();

		assertThatThrownBy(() -> new GrupoOfertado("1", nueve)).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void losBloquesDeUnMismoGrupoNoSePuedenSolapar() {
		assertThatThrownBy(() -> grupo("1", bloque(DiaDeLaSemana.LUNES, "08:00", "10:00"), bloque(DiaDeLaSemana.LUNES, "09:00", "11:00")))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("se solapan");
	}

	@Test
	void dosGruposChocanSiAlgunoDeSusBloquesChoca() {
		GrupoOfertado a = grupo("1", bloque(DiaDeLaSemana.LUNES, "08:00", "10:00"), bloque(DiaDeLaSemana.MIERCOLES, "08:00", "10:00"));
		GrupoOfertado b = grupo("1", bloque(DiaDeLaSemana.MARTES, "08:00", "10:00"), bloque(DiaDeLaSemana.MIERCOLES, "09:00", "11:00"));
		GrupoOfertado c = grupo("2", bloque(DiaDeLaSemana.JUEVES, "08:00", "10:00"));

		assertThat(a.chocaCon(b)).isTrue();
		assertThat(a.chocaCon(c)).isFalse();
	}

	// --- Asignatura ------------------------------------------------------------------------------------------

	@Test
	void unaAsignaturaNecesitaNombreYGrupos() {
		GrupoOfertado g = grupo("1", bloque(DiaDeLaSemana.LUNES, "08:00", "10:00"));

		assertThatThrownBy(() -> new AsignaturaOfertada("", List.of(g))).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new AsignaturaOfertada("Cálculo", List.of())).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new AsignaturaOfertada("x".repeat(81), List.of(g))).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThat(new AsignaturaOfertada("  Cálculo ", List.of(g)).nombre()).isEqualTo("Cálculo");
	}

	@Test
	void losCodigosDeLosGruposDeUnaAsignaturaNoSeRepiten() {
		GrupoOfertado uno = grupo("A", bloque(DiaDeLaSemana.LUNES, "08:00", "10:00"));
		GrupoOfertado otro = grupo("a", bloque(DiaDeLaSemana.MARTES, "08:00", "10:00"));

		assertThatThrownBy(() -> new AsignaturaOfertada("Cálculo", List.of(uno, otro)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("repite");
	}

	@Test
	void unaAsignaturaNoAdmiteMasDeDoceGrupos() {
		List<GrupoOfertado> trece = IntStream.range(0, 13)
				.mapToObj(i -> grupo("g" + i, bloque(DiaDeLaSemana.LUNES, "08:00", "10:00")))
				.toList();

		assertThatThrownBy(() -> new AsignaturaOfertada("Cálculo", trece)).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	// --- Criterio --------------------------------------------------------------------------------------------

	@Test
	void sinNadaPedidoElCriterioNoPideNada() {
		assertThat(CriterioDeAcomodo.ninguno().pideAlgo()).isFalse();
		assertThat(new CriterioDeAcomodo(null, false, null).diasLibres()).isEmpty();
		assertThat(new CriterioDeAcomodo(FranjaDelDia.MANANA, false, null).pideAlgo()).isTrue();
		assertThat(new CriterioDeAcomodo(null, true, null).pideAlgo()).isTrue();
		assertThat(new CriterioDeAcomodo(null, false, java.util.Set.of(DiaDeLaSemana.VIERNES)).pideAlgo()).isTrue();
	}
}
