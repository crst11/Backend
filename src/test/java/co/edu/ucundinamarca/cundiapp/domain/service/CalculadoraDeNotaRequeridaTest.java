package co.edu.ucundinamarca.cundiapp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeEntrega;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.OrigenDeCategoria;
import co.edu.ucundinamarca.cundiapp.domain.model.ProyeccionDeNota;
import co.edu.ucundinamarca.cundiapp.domain.model.SituacionDeLaMeta;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeActividad;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * La nota que hace falta para llegar a la meta (RF08, SCRUM-33) y la simulación con notas hipotéticas
 * (SCRUM-34).
 *
 * <p>Pesos reales de la estructura de prueba: Parcial 21 %, Taller 9 %, Proyecto 30 % y el tercer corte
 * (40 %) todavía sin desglosar, así que nunca se puede calificar.
 */
class CalculadoraDeNotaRequeridaTest {

	private static final int MATRICULA = 7;

	private static EstructuraDeEvaluacion estructura() {
		return new EstructuraDeEvaluacion(MATRICULA, List.of(
				new CategoriaDeEvaluacion(1, "Primer corte", new BigDecimal("30"), OrigenDeCategoria.ESTUDIANTE,
						List.of(actividad(1, "Parcial", "70"), actividad(2, "Taller", "30"))),
				new CategoriaDeEvaluacion(2, "Segundo corte", new BigDecimal("30"), OrigenDeCategoria.ESTUDIANTE,
						List.of(actividad(1, "Proyecto", "100"))),
				new CategoriaDeEvaluacion(3, "Tercer corte", new BigDecimal("40"), OrigenDeCategoria.ESTUDIANTE,
						List.of())));
	}

	/** Todo desglosado: una sola actividad que vale la materia entera. */
	private static EstructuraDeEvaluacion estructuraCompleta() {
		return new EstructuraDeEvaluacion(MATRICULA, List.of(
				new CategoriaDeEvaluacion(1, "Examen final", new BigDecimal("100"), OrigenDeCategoria.ESTUDIANTE,
						List.of(actividad(1, "Examen", "100")))));
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

	private static Meta meta(String valor) {
		return new Meta(new BigDecimal(valor));
	}

	private final CalculadoraDeNotaRequerida calculadora = new CalculadoraDeNotaRequerida(new CalculadoraDeNotas());

	@Test
	void sinNingunaNotaHaceFaltaLaMetaCompletaEnTodoLoQueQueda() {
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), notas(), meta("3.0"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.ALCANZABLE);
		assertThat(proyeccion.puntos()).isEqualByComparingTo("0");
		assertThat(proyeccion.porcentajeEvaluado()).isEqualByComparingTo("0");
		assertThat(proyeccion.porcentajeRestante()).isEqualByComparingTo("100");
		assertThat(proyeccion.requerida()).hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("3.00"));
		assertThat(proyeccion.maximoPosible()).isEqualByComparingTo("5.00");
	}

	@Test
	void conUnaNotaParcialLaRequeridaSeRedondeaHaciaArribaPorqueHaciaAbajoNoAlcanza() {
		// Parcial 4.0 -> 0.84 ganado y 21 % evaluado. Falta (3.0 - 0.84) / 0.79 = 2.7342...
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), notas(1, 1, "4.0"), meta("3.0"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.ALCANZABLE);
		assertThat(proyeccion.puntos()).isEqualByComparingTo("0.84");
		assertThat(proyeccion.porcentajeEvaluado()).isEqualByComparingTo("21");
		assertThat(proyeccion.porcentajeRestante()).isEqualByComparingTo("79");
		assertThat(proyeccion.requerida()).hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("2.74"));
		assertThat(proyeccion.maximoPosible()).isEqualByComparingTo("4.79");
	}

	@Test
	void unaDivisionExactaNoSeInflaAlRedondear() {
		// Parcial 4.0 y Taller 3.0 -> 1.11 ganado, 30 % evaluado. Falta (3.0 - 1.11) / 0.70 = 2.70 justo.
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), notas(1, 1, "4.0", 1, 2, "3.0"), meta("3.0"));

		assertThat(proyeccion.puntos()).isEqualByComparingTo("1.11");
		assertThat(proyeccion.requerida()).hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("2.70"));
	}

	@Test
	void conLaMetaYaCubiertaNoHaceFaltaNada() {
		// Parcial, Taller y Proyecto en 5.0 -> 5.0 x 0.60 = 3.00, justo la meta.
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(),
				notas(1, 1, "5.0", 1, 2, "5.0", 2, 1, "5.0"), meta("3.0"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.ALCANZADA);
		assertThat(proyeccion.puntos()).isEqualByComparingTo("3.00");
		assertThat(proyeccion.porcentajeRestante()).isEqualByComparingTo("40");
		assertThat(proyeccion.requerida()).hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("0"));
	}

	@Test
	void unaMetaDeCeroSeAlcanzaDesdeElPrimerDia() {
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), notas(), meta("0"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.ALCANZADA);
	}

	@Test
	void cuandoNiConCincoEnTodoLoQueQuedaSeLlegaDiceQueNoEsAlcanzableYNoInventaUnaNota() {
		// Tres ceros: quedan 40 % por evaluar, máximo posible 5.0 x 0.40 = 2.00, y la meta es 4.5.
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(),
				notas(1, 1, "0.0", 1, 2, "0.0", 2, 1, "0.0"), meta("4.5"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.INALCANZABLE);
		assertThat(proyeccion.requerida()).isEmpty();
		assertThat(proyeccion.maximoPosible()).isEqualByComparingTo("2.00");
	}

	@Test
	void pedirCincoEnTodoLoQueQuedaSigueSiendoAlcanzable() {
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), notas(), meta("5.0"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.ALCANZABLE);
		assertThat(proyeccion.requerida()).hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("5.00"));
		assertThat(proyeccion.maximoPosible()).isEqualByComparingTo("5.00");
	}

	@Test
	void sinNadaPorEvaluarYSinHaberLlegadoLaMetaYaNoSePuedeAlcanzar() {
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructuraCompleta(), notas(1, 1, "2.5"), meta("3.0"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.INALCANZABLE);
		assertThat(proyeccion.porcentajeRestante()).isEqualByComparingTo("0");
		assertThat(proyeccion.requerida()).isEmpty();
		assertThat(proyeccion.maximoPosible()).isEqualByComparingTo("2.50");
	}

	@Test
	void sinNadaPorEvaluarYHabiendoLlegadoLaMetaEstaAlcanzada() {
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructuraCompleta(), notas(1, 1, "3.0"), meta("3.0"));

		assertThat(proyeccion.situacion()).isEqualTo(SituacionDeLaMeta.ALCANZADA);
	}

	@Test
	void unaNotaSimuladaSeComportaIgualQueUnaReal() {
		NotasDeLaAsignatura simuladas = notas().conSimuladas(
				Map.of(new ClaveDeActividad(1, 1), new Nota(new BigDecimal("4.0"))));

		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), simuladas, meta("3.0"));

		assertThat(proyeccion.puntos()).isEqualByComparingTo("0.84");
		assertThat(proyeccion.requerida()).hasValueSatisfying(r -> assertThat(r).isEqualByComparingTo("2.74"));
	}

	@Test
	void unaNotaSimuladaReemplazaALaRealDeLaMismaActividadSinTocarLasReales() {
		NotasDeLaAsignatura reales = notas(1, 1, "2.0");
		NotasDeLaAsignatura simuladas = reales.conSimuladas(
				Map.of(new ClaveDeActividad(1, 1), new Nota(new BigDecimal("5.0"))));

		ProyeccionDeNota conSimulacion = calculadora.proyectar(estructura(), simuladas, meta("3.0"));
		ProyeccionDeNota sinSimulacion = calculadora.proyectar(estructura(), reales, meta("3.0"));

		// 5.0 x 0.21 = 1.05 frente a 2.0 x 0.21 = 0.42: lo real no se modifica.
		assertThat(conSimulacion.puntos()).isEqualByComparingTo("1.05");
		assertThat(sinSimulacion.puntos()).isEqualByComparingTo("0.42");
	}

	@Test
	void lasSimuladasSeSumanALasRealesDeOtrasActividades() {
		NotasDeLaAsignatura mezcla = notas(1, 1, "4.0").conSimuladas(
				Map.of(new ClaveDeActividad(1, 2), new Nota(new BigDecimal("3.0"))));

		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), mezcla, meta("3.0"));

		assertThat(proyeccion.puntos()).isEqualByComparingTo("1.11");
		assertThat(proyeccion.porcentajeEvaluado()).isEqualByComparingTo("30");
	}

	@Test
	void laMetaSeGuardaTalCualYSeDevuelveEnLaProyeccion() {
		ProyeccionDeNota proyeccion = calculadora.proyectar(estructura(), notas(), meta("3.5"));

		assertThat(proyeccion.meta()).isEqualTo(meta("3.5"));
	}
}
