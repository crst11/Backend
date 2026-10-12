package co.edu.ucundinamarca.cundiapp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEntrega;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.OrigenDeCategoria;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeActividad;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * La nota acumulada de una asignatura (RF05, SCRUM-28): lo que el estudiante lleva ganado según el
 * peso real de cada actividad en la nota final.
 *
 * <p>El peso real es el del corte por el de la actividad dentro del corte: un parcial que vale 70 %
 * de un corte que vale 30 % pesa 21 % de la materia.
 */
class CalculadoraDeNotasTest {

	private static final int MATRICULA = 7;

	/**
	 * Primer corte 30 % (Parcial 70 %, Taller 30 %), segundo corte 30 % (Proyecto 100 %) y tercer
	 * corte 40 % todavía sin desglosar.
	 */
	private static EstructuraDeEvaluacion estructura() {
		return new EstructuraDeEvaluacion(MATRICULA, List.of(
				new CategoriaDeEvaluacion(1, "Primer corte", new BigDecimal("30"), OrigenDeCategoria.ESTUDIANTE,
						List.of(actividad(1, "Parcial", "70"), actividad(2, "Taller", "30"))),
				new CategoriaDeEvaluacion(2, "Segundo corte", new BigDecimal("30"), OrigenDeCategoria.ESTUDIANTE,
						List.of(actividad(1, "Proyecto", "100"))),
				new CategoriaDeEvaluacion(3, "Tercer corte", new BigDecimal("40"), OrigenDeCategoria.ESTUDIANTE,
						List.of())));
	}

	private static ActividadEvaluativa actividad(int consecutivo, String nombre, String porcentaje) {
		return new ActividadEvaluativa(consecutivo, nombre, new BigDecimal(porcentaje), null, TipoDeActividad.TALLER,
				EstadoDeEntrega.NO_ENTREGADA);
	}

	/** Terna a terna: categoría, actividad y nota. */
	private static NotasDeLaAsignatura notas(Object... clavesYNotas) {
		Map<ClaveDeActividad, Nota> mapa = new HashMap<>();
		for (int i = 0; i < clavesYNotas.length; i += 3) {
			mapa.put(new ClaveDeActividad((int) clavesYNotas[i], (int) clavesYNotas[i + 1]),
					new Nota(new BigDecimal((String) clavesYNotas[i + 2])));
		}
		return new NotasDeLaAsignatura(MATRICULA, mapa);
	}

	private final CalculadoraDeNotas calculadora = new CalculadoraDeNotas();

	@Test
	void sinNingunaNotaNoLlevaNadaGanadoNiEvaluado() {
		NotaAcumulada acumulada = calculadora.acumular(estructura(), notas());

		assertThat(acumulada.puntos()).isEqualByComparingTo("0");
		assertThat(acumulada.porcentajeEvaluado()).isEqualByComparingTo("0");
		// Con nada evaluado no hay promedio: decir 0.0 sería inventar una nota que nadie puso.
		assertThat(acumulada.promedioDeLoEvaluado()).isEmpty();
		assertThat(acumulada.actividadesCalificadas()).isZero();
		assertThat(acumulada.actividadesTotales()).isEqualTo(3);
	}

	@Test
	void unaNotaPesaLoQueValeSuCorteMultiplicadoPorLoQueValeEnElCorte() {
		// Parcial 4.0: pesa 30 % x 70 % = 21 % de la materia, y aporta 4.0 x 0.21 = 0.84.
		NotaAcumulada acumulada = calculadora.acumular(estructura(), notas(1, 1, "4.0"));

		assertThat(acumulada.puntos()).isEqualByComparingTo("0.84");
		assertThat(acumulada.porcentajeEvaluado()).isEqualByComparingTo("21");
		assertThat(acumulada.promedioDeLoEvaluado()).hasValueSatisfying(promedio ->
				assertThat(promedio).isEqualByComparingTo("4.0"));
		assertThat(acumulada.actividadesCalificadas()).isEqualTo(1);
	}

	@Test
	void sumaLasNotasDeVariosCortesYElPromedioEsElDeLoYaEvaluado() {
		// Parcial 4.0 (21 %) + Proyecto 3.0 (30 %): 0.84 + 0.90 = 1.74 sobre 51 % evaluado = 3.41.
		NotaAcumulada acumulada = calculadora.acumular(estructura(), notas(1, 1, "4.0", 2, 1, "3.0"));

		assertThat(acumulada.puntos()).isEqualByComparingTo("1.74");
		assertThat(acumulada.porcentajeEvaluado()).isEqualByComparingTo("51");
		assertThat(acumulada.promedioDeLoEvaluado()).hasValueSatisfying(promedio ->
				assertThat(promedio).isEqualByComparingTo("3.41"));
		assertThat(acumulada.actividadesCalificadas()).isEqualTo(2);
	}

	@Test
	void unCorteSinDesglosarNoSeCuentaComoEvaluadoAunqueValga40() {
		// Aunque el tercer corte vale 40 %, sin actividades no hay nada que calificar en él.
		NotaAcumulada acumulada = calculadora.acumular(estructura(),
				notas(1, 1, "5.0", 1, 2, "5.0", 2, 1, "5.0"));

		assertThat(acumulada.porcentajeEvaluado()).isEqualByComparingTo("60");
		assertThat(acumulada.puntos()).isEqualByComparingTo("3.00");
		assertThat(acumulada.promedioDeLoEvaluado()).hasValueSatisfying(promedio ->
				assertThat(promedio).isEqualByComparingTo("5.0"));
	}

	@Test
	void redondeaAlFinalYNoPasoAPaso() {
		// Taller 3.3 pesa 30 % x 30 % = 9 %: aporta 0.297, que se muestra 0.30.
		NotaAcumulada acumulada = calculadora.acumular(estructura(), notas(1, 2, "3.3"));

		assertThat(acumulada.puntos()).isEqualByComparingTo("0.30");
		assertThat(acumulada.promedioDeLoEvaluado()).hasValueSatisfying(promedio ->
				assertThat(promedio).isEqualByComparingTo("3.30"));
	}

	@Test
	void unaNotaDeUnaActividadQueYaNoExisteNoCuenta() {
		NotaAcumulada acumulada = calculadora.acumular(estructura(), notas(9, 9, "5.0"));

		assertThat(acumulada.actividadesCalificadas()).isZero();
		assertThat(acumulada.puntos()).isEqualByComparingTo("0");
	}

	@Test
	void unaEstructuraSinActividadesNoTieneNadaQueCalificar() {
		var vacia = new EstructuraDeEvaluacion(MATRICULA, List.of());

		NotaAcumulada acumulada = calculadora.acumular(vacia, notas());

		assertThat(acumulada.actividadesTotales()).isZero();
		assertThat(acumulada.promedioDeLoEvaluado()).isEmpty();
	}
}
