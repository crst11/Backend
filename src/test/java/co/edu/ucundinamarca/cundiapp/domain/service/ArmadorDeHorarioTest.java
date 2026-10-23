package co.edu.ucundinamarca.cundiapp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaOfertada;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeOferta;
import co.edu.ucundinamarca.cundiapp.domain.model.CriterioDeAcomodo;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.FranjaDelDia;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.model.PropuestaDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeAcomodo;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** El motor que arma combinaciones de grupos sin choques y las ordena por lo que el estudiante prefiere (RF13). */
class ArmadorDeHorarioTest {

	private static final DiaDeLaSemana LUN = DiaDeLaSemana.LUNES;
	private static final DiaDeLaSemana MAR = DiaDeLaSemana.MARTES;
	private static final DiaDeLaSemana VIE = DiaDeLaSemana.VIERNES;

	private final ArmadorDeHorario armador = new ArmadorDeHorario();

	private static BloqueDeOferta bloque(DiaDeLaSemana dia, String inicio, String fin) {
		return new BloqueDeOferta(dia, LocalTime.parse(inicio), LocalTime.parse(fin));
	}

	private static GrupoOfertado grupo(String codigo, BloqueDeOferta... bloques) {
		return new GrupoOfertado(codigo, List.of(bloques));
	}

	private static AsignaturaOfertada materia(String nombre, GrupoOfertado... grupos) {
		return new AsignaturaOfertada(nombre, List.of(grupos));
	}

	private static String grupoElegido(PropuestaDeHorario propuesta, String asignatura) {
		return propuesta.elecciones().stream().filter(e -> e.asignatura().equals(asignatura)).findFirst().orElseThrow().grupo();
	}

	// --- Choques ---------------------------------------------------------------------------------------------

	@Test
	void unaSolaMateriaConUnGrupoDevuelveEsaUnicaPropuesta() {
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")))),
				CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).hasSize(1);
		assertThat(grupoElegido(r.propuestas().getFirst(), "Cálculo")).isEqualTo("1");
		assertThat(r.choques()).isEmpty();
		assertThat(r.mensaje()).isNull();
		assertThat(r.busquedaIncompleta()).isFalse();
	}

	@Test
	void losGruposQueSeTocanSinSolaparseSePuedenJuntar() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00"))),
				materia("Física", grupo("1", bloque(LUN, "10:00", "12:00")))), CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).hasSize(1);
	}

	@Test
	void losGruposQueSeSolapanSeDescartan() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")), grupo("2", bloque(LUN, "14:00", "16:00"))),
				materia("Física", grupo("1", bloque(LUN, "09:00", "11:00")))), CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).hasSize(1);
		assertThat(grupoElegido(r.propuestas().getFirst(), "Cálculo")).isEqualTo("2");
		assertThat(grupoElegido(r.propuestas().getFirst(), "Física")).isEqualTo("1");
	}

	// --- Preferencias ----------------------------------------------------------------------------------------

	@Test
	void conPreferenciaDeMananaVaPrimeroLaCombinacionConMasClasesEnLaManana() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("tarde", bloque(LUN, "14:00", "16:00")), grupo("manana", bloque(LUN, "08:00", "10:00"))),
				materia("Física", grupo("1", bloque(MAR, "08:00", "10:00")))),
				new CriterioDeAcomodo(FranjaDelDia.MANANA, false, null));

		assertThat(r.propuestas()).hasSize(2);
		assertThat(grupoElegido(r.propuestas().get(0), "Cálculo")).isEqualTo("manana");
		assertThat(r.propuestas().get(0).puntaje()).isEqualTo(100);
		assertThat(r.propuestas().get(1).puntaje()).isEqualTo(50);
	}

	@Test
	void elPuntajeDeLaFranjaEsElPorcentajeDeMinutosQueCaenEnEllaSobreCien() {
		// 2 horas en la mañana y 2 en la tarde: la mitad.
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00"))),
				materia("Física", grupo("1", bloque(MAR, "14:00", "16:00")))),
				new CriterioDeAcomodo(FranjaDelDia.MANANA, false, null));

		assertThat(r.propuestas().getFirst().puntaje()).isEqualTo(50);
	}

	@Test
	void conPocosHuecosVanPrimeroLasClasesPegadas() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00"))),
				materia("Física", grupo("lejos", bloque(LUN, "14:00", "16:00")), grupo("pegada", bloque(LUN, "10:00", "12:00")))),
				new CriterioDeAcomodo(null, true, null));

		assertThat(grupoElegido(r.propuestas().get(0), "Física")).isEqualTo("pegada");
		assertThat(r.propuestas().get(0).puntaje()).isEqualTo(100);
		assertThat(r.propuestas().get(0).resumen().minutosDeHuecos()).isZero();
		// 4 horas libres entre clases: de 600 minutos de referencia queda el 60 %.
		assertThat(r.propuestas().get(1).resumen().minutosDeHuecos()).isEqualTo(240);
		assertThat(r.propuestas().get(1).puntaje()).isEqualTo(60);
	}

	@Test
	void conDiasLibresVanPrimeroLasCombinacionesSinClasesEsosDias() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("viernes", bloque(VIE, "08:00", "10:00")), grupo("lunes", bloque(LUN, "08:00", "10:00")))),
				new CriterioDeAcomodo(null, false, Set.of(VIE)));

		assertThat(grupoElegido(r.propuestas().get(0), "Cálculo")).isEqualTo("lunes");
		assertThat(r.propuestas().get(0).puntaje()).isEqualTo(100);
		assertThat(r.propuestas().get(0).resumen().diasLibres()).contains(VIE);
		assertThat(r.propuestas().get(1).puntaje()).isZero();
	}

	@Test
	void losCriteriosPedidosSeRepartenElPesoParaQueElMaximoSigaSiendoCien() {
		// Franja (60) y huecos (25): quien cumple solo los huecos vale 25 de 85.
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("manana", bloque(LUN, "08:00", "10:00")), grupo("tarde", bloque(LUN, "14:00", "16:00")))),
				new CriterioDeAcomodo(FranjaDelDia.MANANA, true, null));

		assertThat(r.propuestas().get(0).puntaje()).isEqualTo(100);
		assertThat(r.propuestas().get(1).puntaje()).isEqualTo(29);
	}

	@Test
	void sinNadaPedidoTodasValenLoMismoYSeRespetaElOrdenEnQueSeEscribio() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("a", bloque(LUN, "08:00", "10:00")), grupo("b", bloque(MAR, "08:00", "10:00")),
						grupo("c", bloque(VIE, "08:00", "10:00")), grupo("d", bloque(DiaDeLaSemana.SABADO, "08:00", "10:00")))),
				CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).hasSize(3);
		assertThat(r.propuestas()).extracting(p -> grupoElegido(p, "Cálculo")).containsExactly("a", "b", "c");
		assertThat(r.propuestas()).extracting(PropuestaDeHorario::puntaje).containsOnly(100);
	}

	@Test
	void conPuntajesIgualesMandaElOrdenDeEntradaYElResultadoEsElMismoSiempre() {
		List<AsignaturaOfertada> oferta = List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")), grupo("2", bloque(MAR, "08:00", "10:00"))),
				materia("Física", grupo("1", bloque(LUN, "10:00", "12:00")), grupo("2", bloque(MAR, "10:00", "12:00"))));
		CriterioDeAcomodo criterio = new CriterioDeAcomodo(FranjaDelDia.MANANA, true, null);

		assertThat(armador.proponer(oferta, criterio)).isEqualTo(armador.proponer(oferta, criterio));
	}

	// --- Resumen y motivos -----------------------------------------------------------------------------------

	@Test
	void laPropuestaCuentaLosMinutosPorFranjaLosDiasConClaseYLosLibres() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "11:00", "13:00"), bloque(MAR, "19:00", "21:00")))),
				CriterioDeAcomodo.ninguno());

		PropuestaDeHorario.Resumen resumen = r.propuestas().getFirst().resumen();
		assertThat(resumen.minutosEnLaManana()).isEqualTo(60);
		assertThat(resumen.minutosEnLaTarde()).isEqualTo(60);
		assertThat(resumen.minutosEnLaNoche()).isEqualTo(120);
		assertThat(resumen.diasConClase()).containsExactly(LUN, MAR);
		assertThat(resumen.diasLibres()).containsExactly(DiaDeLaSemana.MIERCOLES, DiaDeLaSemana.JUEVES, VIE, DiaDeLaSemana.SABADO);
	}

	@Test
	void laPropuestaTraeLosBloquesDeCadaGrupoElegidoParaQuePantallaSoloLosMuestre() {
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")))),
				CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas().getFirst().elecciones().getFirst().bloques()).containsExactly(bloque(LUN, "08:00", "10:00"));
	}

	@Test
	void losMotivosDicenEnPalabrasPorQueSeProponeEsaCombinacion() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00"))),
				materia("Física", grupo("1", bloque(LUN, "12:00", "14:00")))),
				new CriterioDeAcomodo(FranjaDelDia.MANANA, true, Set.of(VIE, MAR)));

		assertThat(r.propuestas().getFirst().motivos()).containsExactly(
				"Sin choques entre tus 2 materias.",
				"El 50 % de tus horas de clase queda en la mañana.",
				"Tienes 2 h libres entre clases en toda la semana.",
				"Te quedan libres: martes y viernes.");
	}

	@Test
	void losMotivosAdviertenLoQueNoSeLogro() {
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("Cálculo", grupo("1", bloque(VIE, "14:00", "16:00")))),
				new CriterioDeAcomodo(FranjaDelDia.MANANA, true, Set.of(VIE)));

		assertThat(r.propuestas().getFirst().motivos()).containsExactly(
				"Una sola materia, sin choques.",
				"Ninguna de tus clases queda en la mañana.",
				"No tienes huecos entre clases.",
				"No se logró dejar libre: viernes.");
	}

	@Test
	void devuelveComoMaximoTresPropuestas() {
		List<GrupoOfertado> grupos = IntStream.range(0, 6)
				.mapToObj(i -> grupo("g" + i, bloque(DiaDeLaSemana.values()[i % 6], "08:00", "10:00")))
				.toList();

		ResultadoDeAcomodo r = armador.proponer(List.of(new AsignaturaOfertada("Cálculo", grupos)), CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).hasSize(3);
	}

	// --- Sin solución ----------------------------------------------------------------------------------------

	@Test
	void sinSolucionDiceQueMateriasChocanEnTodosSusGrupos() {
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00"))),
				materia("Física", grupo("1", bloque(LUN, "09:00", "11:00"))),
				materia("Inglés", grupo("1", bloque(MAR, "08:00", "10:00")))), CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).isEmpty();
		assertThat(r.choques()).containsExactly(new ResultadoDeAcomodo.Choque("Cálculo", "Física"));
		assertThat(r.mensaje()).contains("«Cálculo» y «Física»").contains("todos sus grupos chocan");
	}

	@Test
	void sinSolucionSinUnaParejaClaraDaUnMensajeGeneral() {
		// Cada pareja se puede juntar, pero tres materias no caben en dos horarios.
		GrupoOfertado a = grupo("1", bloque(LUN, "08:00", "10:00"));
		GrupoOfertado b = grupo("2", bloque(MAR, "08:00", "10:00"));
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("A", a, b), materia("B", a, b), materia("C", a, b)),
				CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).isEmpty();
		assertThat(r.choques()).isEmpty();
		assertThat(r.mensaje()).contains("Ninguna combinación");
	}

	@Test
	void siElTopeDeBusquedaSeAlcanzaLoDiceYNoAfirmaQueNoHaySolucion() {
		List<AsignaturaOfertada> oferta = IntStream.range(0, 4)
				.mapToObj(i -> new AsignaturaOfertada("M" + i, IntStream.range(0, 6)
						.mapToObj(g -> grupo("g" + g, bloque(DiaDeLaSemana.values()[g], "%02d:00".formatted(6 + i), "%02d:00".formatted(7 + i))))
						.toList()))
				.toList();

		ResultadoDeAcomodo r = new ArmadorDeHorario(10).proponer(oferta, CriterioDeAcomodo.ninguno());

		assertThat(r.busquedaIncompleta()).isTrue();
		assertThat(r.propuestas()).isNotEmpty();
	}

	@Test
	void unaOfertaEnormeSinChoquesTerminaRapidoPorElTopeDeBusqueda() {
		// El peor caso que la API admite: 10 materias de 20 grupos, 8 bloques cada uno, sin un solo choque.
		List<AsignaturaOfertada> oferta = IntStream.range(0, 10)
				.mapToObj(m -> new AsignaturaOfertada("M" + m, IntStream.range(0, 20)
						.mapToObj(g -> new GrupoOfertado("g" + g, IntStream.range(0, 8)
								// Seis bloques a la hora en punto de cada día y dos a la media, sin solaparse entre sí ni con otra materia.
								.mapToObj(b -> b < 6
										? bloque(DiaDeLaSemana.values()[b], "%02d:00".formatted(6 + m), "%02d:25".formatted(6 + m))
										: bloque(DiaDeLaSemana.values()[b - 6], "%02d:30".formatted(6 + m), "%02d:55".formatted(6 + m)))
								.toList()))
						.toList()))
				.toList();

		long inicio = System.nanoTime();
		ResultadoDeAcomodo r = armador.proponer(oferta, new CriterioDeAcomodo(FranjaDelDia.MANANA, true, Set.of(VIE)));
		long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

		assertThat(r.busquedaIncompleta()).isTrue();
		assertThat(r.propuestas()).isNotEmpty();
		// Lo que garantiza que la busqueda se detiene es el tope de pasos (MAXIMO_DE_PASOS), y eso ya lo comprueba busquedaIncompleta().
		// El tiempo solo vigila que cada paso siga siendo barato, y por eso el limite es holgado: tarda unos 0,7 s en un equipo de desarrollo y
		// llego a 3,1 s en un runner compartido y cargado (SCRUM-239: con un limite de 3 s fallo sin que el codigo hubiera cambiado). Una
		// explosion de verdad (explorar todas las combinaciones) tardaria minutos u horas. Medido: con un tope 30 veces mayor (6 millones de
		// pasos) la prueba aun pasa y con uno 300 veces mayor (60 millones) falla por tiempo, asi que atrapa una busqueda sin freno sin
		// depender de la velocidad de la maquina.
		assertThat(milisegundos).as("tiempo del peor caso, en ms").isLessThan(15_000);
	}

	@Test
	void siElTopeSeAlcanzaSinHallarNingunaPropuestaElMensajeNoDiceQueSeaImposible() {
		GrupoOfertado a = grupo("1", bloque(LUN, "08:00", "10:00"));
		GrupoOfertado b = grupo("2", bloque(MAR, "08:00", "10:00"));
		ResultadoDeAcomodo r = new ArmadorDeHorario(3).proponer(List.of(materia("A", a, b), materia("B", a, b), materia("C", a, b)),
				CriterioDeAcomodo.ninguno());

		assertThat(r.propuestas()).isEmpty();
		assertThat(r.busquedaIncompleta()).isTrue();
		assertThat(r.mensaje()).contains("tiempo de búsqueda");
	}

	// --- Bordes que las pruebas de mutación (SCRUM-242) encontraron sin vigilar -------------------------------------

	@Test
	void losHuecosSeMidenEnOrdenDeHoraAunqueLasMateriasLleguenDesordenadas() {
		// Cálculo (10:00) se escribió antes que Física (08:00): entre las 9 y las 10 hay una hora libre.
		ResultadoDeAcomodo r = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "10:00", "11:00"))),
				materia("Física", grupo("1", bloque(LUN, "08:00", "09:00")))),
				new CriterioDeAcomodo(null, true, null));

		assertThat(r.propuestas().getFirst().resumen().minutosDeHuecos()).isEqualTo(60);
	}

	@Test
	void elPuntajeDeLosDiasLibresEsLaFraccionDeLosPedidosQueQuedaronLibres() {
		// Pidió libres el lunes y el martes; hay clase el lunes: se cumplió uno de dos.
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")))),
				new CriterioDeAcomodo(null, false, Set.of(LUN, MAR)));

		assertThat(r.propuestas().getFirst().puntaje()).isEqualTo(50);
	}

	@Test
	void unCandidatoMejorQueLlegaConLaListaLlenaDesplazaAlPeor() {
		// Cinco grupos: los tres primeros caen el lunes (el día que quería libre, puntaje 0) y los dos últimos el martes (100).
		// Aunque lleguen cuando ya hay tres propuestas, tienen que entrar y quedar primero.
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("Cálculo",
				grupo("a", bloque(LUN, "08:00", "10:00")), grupo("b", bloque(LUN, "10:00", "12:00")),
				grupo("c", bloque(LUN, "14:00", "16:00")), grupo("d", bloque(MAR, "08:00", "10:00")),
				grupo("e", bloque(MAR, "10:00", "12:00")))),
				new CriterioDeAcomodo(null, false, Set.of(LUN)));

		assertThat(r.propuestas()).extracting(p -> grupoElegido(p, "Cálculo")).containsExactly("d", "e", "a");
	}

	@Test
	void losMotivosDicenLosHuecosEnMinutosYEnHoras() {
		// 45 minutos entre las 9:00 y las 9:45, y luego 1 h 30 min libres entre las 10:45 y las 12:15.
		ResultadoDeAcomodo corto = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "09:00"))),
				materia("Física", grupo("1", bloque(LUN, "09:45", "10:45")))),
				new CriterioDeAcomodo(null, true, null));
		assertThat(corto.propuestas().getFirst().motivos()).contains("Tienes 45 min libres entre clases en toda la semana.");

		ResultadoDeAcomodo mixto = armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "09:00"))),
				materia("Física", grupo("1", bloque(LUN, "11:30", "12:30")))),
				new CriterioDeAcomodo(null, true, null));
		assertThat(mixto.propuestas().getFirst().motivos()).contains("Tienes 2 h 30 min libres entre clases en toda la semana.");
	}

	@Test
	void sePidieronVariosDiasLibresYSoloSeLogroUnoLoDiceDeLosDosLados() {
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("Cálculo", grupo("1", bloque(VIE, "08:00", "10:00")))),
				new CriterioDeAcomodo(null, false, Set.of(LUN, VIE)));

		assertThat(r.propuestas().getFirst().motivos())
				.contains("Te quedan libres: lunes; no se logró dejar libre: viernes.");
	}

	@Test
	void elTopeDePasosSoloSeAlcanzaAlPasarseDeEl() {
		// Una materia con un grupo recorre exactamente un paso: con un tope de uno, la búsqueda termina completa.
		ResultadoDeAcomodo uno = new ArmadorDeHorario(1).proponer(
				List.of(materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")))), CriterioDeAcomodo.ninguno());
		assertThat(uno.busquedaIncompleta()).isFalse();
		assertThat(uno.propuestas()).hasSize(1);

		// Con dos grupos hacen falta dos pasos: con un tope de uno se corta antes de ver el segundo.
		ResultadoDeAcomodo dos = new ArmadorDeHorario(1).proponer(
				List.of(materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")), grupo("2", bloque(MAR, "08:00", "10:00")))),
				CriterioDeAcomodo.ninguno());
		assertThat(dos.busquedaIncompleta()).isTrue();
	}

	// --- Validación de la oferta -----------------------------------------------------------------------------

	@Test
	void sinMateriasNoHayNadaQueArmar() {
		assertThatThrownBy(() -> armador.proponer(List.of(), CriterioDeAcomodo.ninguno()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> armador.proponer(null, CriterioDeAcomodo.ninguno()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void noAdmiteMasDeDiezMaterias() {
		List<AsignaturaOfertada> once = IntStream.range(0, 11)
				.mapToObj(i -> materia("M" + i, grupo("1", bloque(DiaDeLaSemana.values()[i % 6], "08:00", "10:00"))))
				.toList();

		assertThatThrownBy(() -> armador.proponer(once, CriterioDeAcomodo.ninguno()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("10");
	}

	@Test
	void noAdmiteDosMateriasConElMismoNombre() {
		assertThatThrownBy(() -> armador.proponer(List.of(
				materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00"))),
				materia(" cálculo ", grupo("1", bloque(MAR, "08:00", "10:00")))), CriterioDeAcomodo.ninguno()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class)
				.hasMessageContaining("repite");
	}

	// --- Días de la oferta que quedan sin clase (SCRUM-251) -------------------------------------------------

	@Test
	void laPropuestaDiceQueDiasDeLaOfertaQuedanSinClase() {
		var oferta = List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")), grupo("G2", bloque(MAR, "07:00", "09:00"))),
				materia("B", grupo("G1", bloque(DiaDeLaSemana.MIERCOLES, "07:00", "09:00"))));

		ResultadoDeAcomodo r = armador.proponer(oferta, CriterioDeAcomodo.ninguno());

		var conG1 = r.propuestas().stream().filter(p -> grupoElegido(p, "A").equals("G1")).findFirst().orElseThrow();
		var conG2 = r.propuestas().stream().filter(p -> grupoElegido(p, "A").equals("G2")).findFirst().orElseThrow();
		assertThat(conG1.resumen().diasLiberados()).containsExactly(MAR);
		assertThat(conG1.resumen().diasLibres())
				.containsExactly(MAR, DiaDeLaSemana.JUEVES, VIE, DiaDeLaSemana.SABADO);
		assertThat(conG2.resumen().diasLiberados()).containsExactly(LUN);
	}

	// --- «Quiero tener un día libre» (SCRUM-251) -------------------------------------------------------------

	private static final DiaDeLaSemana MIE = DiaDeLaSemana.MIERCOLES;
	private static final DiaDeLaSemana JUE = DiaDeLaSemana.JUEVES;
	private static final CriterioDeAcomodo DIA_LIBRE = new CriterioDeAcomodo(null, false, null, true);

	/** Tres materias de dos grupos de 2 h: todo el lunes, o repartido entre martes, miércoles y jueves. */
	private static List<AsignaturaOfertada> tresMateriasConLunesOMasDias() {
		return List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")), grupo("G2", bloque(MAR, "07:00", "09:00"))),
				materia("B", grupo("G1", bloque(LUN, "09:00", "11:00")), grupo("G2", bloque(MIE, "07:00", "09:00"))),
				materia("C", grupo("G1", bloque(LUN, "11:00", "13:00")), grupo("G2", bloque(JUE, "07:00", "09:00"))));
	}

	@Test
	void laRepartidaLeGanaALaAmontonadaParaElMismoDiaLibre() {
		ResultadoDeAcomodo r = armador.proponer(tresMateriasConLunesOMasDias(), DIA_LIBRE);

		// Se muestran todas las combinaciones que dejan algún día libre, sin importar cuáles ni cuántos: ocho conjuntos
		// distintos de días libres. Las repartidas que liberan un solo día van primero.
		assertThat(r.propuestas()).hasSize(8);
		var primeras = r.propuestas().subList(0, 4);
		assertThat(primeras).allSatisfy(p -> {
			assertThat(p.puntaje()).isEqualTo(100);
			assertThat(p.resumen().diasLiberados()).hasSize(1);
		});
		assertThat(primeras.stream().flatMap(p -> p.resumen().diasLiberados().stream()))
				.containsExactlyInAnyOrder(LUN, MAR, MIE, JUE);
		// La que amontona todo el lunes también aparece, pero al final y con el puntaje que merece (1/3).
		var ultima = r.propuestas().getLast();
		assertThat(ultima.resumen().diasLiberados()).containsExactly(MAR, MIE, JUE);
		assertThat(ultima.puntaje()).isEqualTo(33);
	}

	@Test
	void unaCombinacionQueLiberaVariosDiasSeMuestraAunqueHayaOpcionesQueLiberanUnoSolo() {
		// (G2, G2) libera el lunes y el martes a la vez; las otras tres combinaciones liberan un día cada una.
		var oferta = List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")), grupo("G2", bloque(MIE, "07:00", "09:00"))),
				materia("B", grupo("G1", bloque(MAR, "07:00", "09:00")), grupo("G2", bloque(MIE, "09:00", "11:00"))));

		ResultadoDeAcomodo r = armador.proponer(oferta, DIA_LIBRE);

		assertThat(r.propuestas().stream().map(p -> p.resumen().diasLiberados()))
				.contains(List.of(LUN, MAR))
				.contains(List.of(MIE))
				.hasSize(4);
	}

	@Test
	void nuncaOfreceMasDelMaximoDeOpciones() {
		// Dos materias con un grupo en cada uno de los seis días: 36 combinaciones y 21 conjuntos distintos de días libres.
		var dias = IntStream.range(0, 6).mapToObj(i -> DiaDeLaSemana.values()[i]).toList();
		var oferta = List.of(
				materia("A", dias.stream().map(d -> grupo("A" + d, bloque(d, "07:00", "09:00"))).toArray(GrupoOfertado[]::new)),
				materia("B", dias.stream().map(d -> grupo("B" + d, bloque(d, "09:00", "11:00"))).toArray(GrupoOfertado[]::new)));

		ResultadoDeAcomodo r = armador.proponer(oferta, DIA_LIBRE);

		assertThat(r.propuestas()).hasSize(ArmadorDeHorario.MAXIMO_DE_OPCIONES_CON_DIA_LIBRE);
		var puntajes = r.propuestas().stream().map(PropuestaDeHorario::puntaje).toList();
		assertThat(puntajes).isSortedAccordingTo(java.util.Comparator.reverseOrder());
	}

	@Test
	void unaCombinacionQueLiberaDosDiasEsUnaSolaOpcion() {
		var oferta = List.of(materia("A",
				grupo("G1", bloque(LUN, "07:00", "09:00")),
				grupo("G2", bloque(MAR, "07:00", "09:00")),
				grupo("G3", bloque(MIE, "07:00", "09:00"))));

		ResultadoDeAcomodo r = armador.proponer(oferta, DIA_LIBRE);

		// Cada grupo libera dos días distintos: tres conjuntos, tres opciones, y cada combinación aparece una sola vez.
		assertThat(r.propuestas()).hasSize(3);
		var conG1 = r.propuestas().stream().filter(p -> grupoElegido(p, "A").equals("G1")).findFirst().orElseThrow();
		var conG2 = r.propuestas().stream().filter(p -> grupoElegido(p, "A").equals("G2")).findFirst().orElseThrow();
		assertThat(conG1.resumen().diasLiberados()).containsExactly(MAR, MIE);
		assertThat(conG2.resumen().diasLiberados()).containsExactly(LUN, MIE);
		assertThat(conG1.resumen().diasLibres()).contains(JUE, VIE, DiaDeLaSemana.SABADO);
		assertThat(conG1.resumen().diasLiberados()).doesNotContain(JUE, VIE, DiaDeLaSemana.SABADO);
	}

	@Test
	void unDiaQueTodosLosGruposUsanNoTieneOpcion() {
		var oferta = List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00"))),
				materia("B", grupo("G1", bloque(MAR, "07:00", "09:00")), grupo("G2", bloque(MIE, "07:00", "09:00"))));

		ResultadoDeAcomodo r = armador.proponer(oferta, DIA_LIBRE);

		assertThat(r.propuestas()).hasSize(2);
		assertThat(r.propuestas()).allSatisfy(p -> assertThat(p.resumen().diasLiberados()).doesNotContain(LUN));
	}

	@Test
	void siNingunDiaSePuedeLiberarLoDiceYSugiereQuitarLaMarca() {
		var oferta = List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00"))),
				materia("B", grupo("G1", bloque(MAR, "07:00", "09:00"))));

		ResultadoDeAcomodo r = armador.proponer(oferta, DIA_LIBRE);

		assertThat(r.propuestas()).isEmpty();
		assertThat(r.mensaje()).contains("no se puede dejar ningún día libre");
		assertThat(r.choques()).isEmpty();
		assertThat(r.busquedaIncompleta()).isFalse();
	}

	@Test
	void elDomingoNuncaEsUnDiaLiberable() {
		// «A» puede ser el lunes o el domingo; «B» siempre el martes. Con el domingo contando, (lunes, martes) liberaría el
		// domingo; sin él, solo (domingo, martes) deja algo libre: el lunes.
		var oferta = List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")), grupo("G2", bloque(DiaDeLaSemana.DOMINGO, "07:00", "09:00"))),
				materia("B", grupo("G1", bloque(MAR, "07:00", "09:00"))));

		ResultadoDeAcomodo r = armador.proponer(oferta, DIA_LIBRE);

		assertThat(r.propuestas()).hasSize(1);
		assertThat(r.propuestas().getFirst().resumen().diasLiberados()).containsExactly(LUN);
		assertThat(r.propuestas()).allSatisfy(p -> assertThat(p.resumen().diasLiberados()).doesNotContain(DiaDeLaSemana.DOMINGO));
	}

	@Test
	void elSabadoSoloCuentaSiAlgunaMateriaTieneClaseEseDia() {
		var sinSabado = List.of(materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")), grupo("G2", bloque(MAR, "07:00", "09:00"))));
		var conSabado = List.of(materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")),
				grupo("G2", bloque(DiaDeLaSemana.SABADO, "07:00", "09:00"))));

		assertThat(armador.proponer(sinSabado, DIA_LIBRE).propuestas())
				.allSatisfy(p -> assertThat(p.resumen().diasLiberados()).doesNotContain(DiaDeLaSemana.SABADO));
		assertThat(armador.proponer(conSabado, DIA_LIBRE).propuestas().stream().flatMap(p -> p.resumen().diasLiberados().stream()))
				.contains(DiaDeLaSemana.SABADO);
	}

	@Test
	void siLaBusquedaSeCortaAntesDeHallarUnDiaLiberableNoDiceQueSeaImposible() {
		var oferta = List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")), grupo("G2", bloque(MAR, "07:00", "09:00"))),
				materia("B", grupo("G1", bloque(MAR, "09:00", "11:00")), grupo("G2", bloque(LUN, "09:00", "11:00"))));

		// Con dos pasos solo se alcanza a ver (G1, G1), que usa lunes y martes; (G1, G2) sí libera el martes.
		ResultadoDeAcomodo r = new ArmadorDeHorario(2).proponer(oferta, DIA_LIBRE);

		assertThat(r.propuestas()).isEmpty();
		assertThat(r.busquedaIncompleta()).isTrue();
		assertThat(r.mensaje()).contains("se cortó").doesNotContain("no se puede dejar ningún día libre");
	}

	@Test
	void diaLibreIgnoraLosDiasLibresDelCriterioViejo() {
		var oferta = tresMateriasConLunesOMasDias();

		assertThat(armador.proponer(oferta, new CriterioDeAcomodo(null, false, Set.of(VIE), true)))
				.isEqualTo(armador.proponer(oferta, DIA_LIBRE));
	}

	@Test
	void conDiaLibreLaFranjaSigueContando() {
		var oferta = List.of(
				materia("A", grupo("G1", bloque(LUN, "07:00", "09:00")), grupo("G2", bloque(LUN, "14:00", "16:00"))),
				materia("B", grupo("G1", bloque(MAR, "07:00", "09:00")), grupo("G2", bloque(LUN, "09:00", "11:00"))));

		ResultadoDeAcomodo r = armador.proponer(oferta, new CriterioDeAcomodo(FranjaDelDia.MANANA, false, null, true));

		assertThat(r.propuestas()).hasSize(1);
		assertThat(grupoElegido(r.propuestas().getFirst(), "A")).isEqualTo("G1");
		assertThat(r.propuestas().getFirst().puntaje()).isEqualTo(100);
	}

	@Test
	void losMotivosNombranLosDiasLiberadosYElDiaMasCargado() {
		ResultadoDeAcomodo r = armador.proponer(tresMateriasConLunesOMasDias(), DIA_LIBRE);

		var queLiberaLunes = r.propuestas().stream()
				.filter(p -> p.resumen().diasLiberados().equals(List.of(LUN))).findFirst().orElseThrow();
		assertThat(queLiberaLunes.motivos())
				.contains("Te quedan libres: lunes.", "Tu día más cargado es el martes, con 2 h.");
	}

	@Test
	void elCriterioNuevoNoRompeLosLlamadosDeTresArgumentos() {
		assertThat(new CriterioDeAcomodo(null, false, null).diaLibre()).isFalse();
		assertThat(new CriterioDeAcomodo(null, false, null, true).diaLibre()).isTrue();
		assertThat(new CriterioDeAcomodo(null, false, null, true).pideAlgo()).isTrue();
	}

	@Test
	void sinCriterioSeTomaComoSinPreferencias() {
		ResultadoDeAcomodo r = armador.proponer(List.of(materia("Cálculo", grupo("1", bloque(LUN, "08:00", "10:00")))), null);

		assertThat(r.propuestas()).hasSize(1);
	}
}
