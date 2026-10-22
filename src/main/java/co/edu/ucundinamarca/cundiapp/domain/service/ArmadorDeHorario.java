package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaOfertada;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeOferta;
import co.edu.ucundinamarca.cundiapp.domain.model.CriterioDeAcomodo;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.FranjaDelDia;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.model.PropuestaDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeAcomodo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Arma las combinaciones de grupos sin choques que el estudiante podría matricular y las ordena según lo que
 * prefiere (RF13, SCRUM-207). No guarda nada y no conoce la base de datos: recibe la oferta que el estudiante
 * escribió y devuelve hasta tres propuestas.
 *
 * <p>Busca por retroceso: elige un grupo por asignatura, en el orden en que se escribieron, y descarta una rama
 * en cuanto un grupo choca con los ya elegidos. Tiene un tope de pasos; si lo alcanza, devuelve lo mejor que
 * encontró y lo dice.
 *
 * <p>El puntaje es de 0 a 100 y solo cuenta lo que el estudiante pidió. Los pesos son franja 60, huecos 25 y días
 * libres 15, repartidos entre los criterios pedidos para que el máximo siga siendo 100. Con puntajes iguales
 * manda el orden en que se escribió la oferta, así el resultado es siempre el mismo.
 */
public class ArmadorDeHorario {

	public static final int MAXIMO_DE_ASIGNATURAS = 10;
	public static final int MAXIMO_DE_PROPUESTAS = 3;
	/**
	 * Tope de la búsqueda. Una oferta normal (6 materias de 4 grupos son unas 4 000 combinaciones) se recorre
	 * completa; el tope existe para que una oferta enorme, la que la API admite como máximo, no tarde segundos.
	 */
	static final int MAXIMO_DE_PASOS = 200_000;
	/** Con estos minutos libres entre clases en la semana, el puntaje de «pocos huecos» llega a cero. */
	static final int HUECOS_QUE_ANULAN_EL_PUNTAJE = 600;

	private static final int PESO_DE_LA_FRANJA = 60;
	private static final int PESO_DE_LOS_HUECOS = 25;
	private static final int PESO_DE_LOS_DIAS_LIBRES = 15;
	/** Con «quiero un día libre»: qué tan repartidas quedan las horas entre los días con clase (SCRUM-251). */
	private static final int PESO_DEL_EQUILIBRIO = 30;
	private static final int DIAS_DE_CLASE = 6;
	/** Con «quiero un día libre» se ofrece una opción por cada conjunto distinto de días libres, hasta ocho (SCRUM-253). */
	public static final int MAXIMO_DE_OPCIONES_CON_DIA_LIBRE = 8;

	private final int maximoDePasos;
	private final RedactorDeAcomodo redactor = new RedactorDeAcomodo();

	public ArmadorDeHorario() {
		this(MAXIMO_DE_PASOS);
	}

	ArmadorDeHorario(int maximoDePasos) {
		this.maximoDePasos = maximoDePasos;
	}

	public ResultadoDeAcomodo proponer(List<AsignaturaOfertada> oferta, CriterioDeAcomodo criterio) {
		validar(oferta);
		CriterioDeAcomodo pedido = criterio == null ? CriterioDeAcomodo.ninguno() : criterio;
		Busqueda busqueda = new Busqueda(oferta, pedido);
		busqueda.recorrer(0);
		List<Candidato> elegidos = pedido.diaLibre() ? busqueda.opcionesConDiaLibre() : busqueda.mejores;
		if (elegidos.isEmpty()) {
			if (pedido.diaLibre() && busqueda.combinacionesValidas > 0) {
				// Si la búsqueda se cortó, que no se haya hallado un día liberable no prueba que no exista.
				String mensaje = busqueda.incompleta ? redactor.mensajeDeTopeSinDiaLibre() : redactor.mensajeSinDiaLibre();
				return new ResultadoDeAcomodo(List.of(), List.of(), mensaje, busqueda.incompleta);
			}
			return sinSolucion(oferta, busqueda.incompleta);
		}
		List<PropuestaDeHorario> propuestas = elegidos.stream()
				.map(candidato -> propuesta(oferta, pedido, candidato, busqueda.medidor))
				.toList();
		return new ResultadoDeAcomodo(propuestas, List.of(), null, busqueda.incompleta);
	}

	private static void validar(List<AsignaturaOfertada> oferta) {
		if (oferta == null || oferta.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Escribe al menos una materia con sus grupos");
		}
		if (oferta.size() > MAXIMO_DE_ASIGNATURAS) {
			throw new ReglaDeNegocioVioladaException("Puedes armar hasta %d materias a la vez".formatted(MAXIMO_DE_ASIGNATURAS));
		}
		Set<String> nombres = new HashSet<>();
		for (AsignaturaOfertada asignatura : oferta) {
			if (!nombres.add(asignatura.nombre().toLowerCase(Locale.ROOT))) {
				throw new ReglaDeNegocioVioladaException(
						"La materia «%s» se repite: escríbela una sola vez con todos sus grupos".formatted(asignatura.nombre()));
			}
		}
	}

	private ResultadoDeAcomodo sinSolucion(List<AsignaturaOfertada> oferta, boolean incompleta) {
		List<ResultadoDeAcomodo.Choque> choques = new ArrayList<>();
		for (int i = 0; i < oferta.size(); i++) {
			for (int j = i + 1; j < oferta.size(); j++) {
				if (todosChocan(oferta.get(i), oferta.get(j))) {
					choques.add(new ResultadoDeAcomodo.Choque(oferta.get(i).nombre(), oferta.get(j).nombre()));
				}
			}
		}
		String mensaje = !choques.isEmpty()
				? redactor.mensajeDeChoques(choques)
				: incompleta ? redactor.mensajeDeTope() : redactor.mensajeGeneral();
		return new ResultadoDeAcomodo(List.of(), choques, mensaje, incompleta);
	}

	private static boolean todosChocan(AsignaturaOfertada una, AsignaturaOfertada otra) {
		for (GrupoOfertado grupo : una.grupos()) {
			for (GrupoOfertado ajeno : otra.grupos()) {
				if (!grupo.chocaCon(ajeno)) {
					return false;
				}
			}
		}
		return true;
	}

	// --- Medir y puntuar -------------------------------------------------------------------------------------

	/** Los números de una combinación, antes de convertirlos en el resumen que ve el estudiante. */
	private record Medida(int[] minutosPorFranja, int huecos, boolean[] diaConClase, int[] minutosPorDia) {

		int total() {
			int suma = 0;
			for (int minutos : minutosPorFranja) {
				suma += minutos;
			}
			return suma;
		}
	}

	/** Un grupo ya convertido a números, para medirlo millones de veces sin crear objetos ni ordenar listas. */
	private record GrupoPlano(int[] dia, int[] inicio, int[] fin, int[] minutosPorFranja) {

		static GrupoPlano de(GrupoOfertado grupo) {
			int n = grupo.bloques().size();
			int[] dia = new int[n];
			int[] inicio = new int[n];
			int[] fin = new int[n];
			int[] porFranja = new int[FranjaDelDia.values().length];
			for (int b = 0; b < n; b++) {
				BloqueDeOferta bloque = grupo.bloques().get(b);
				dia[b] = bloque.dia().ordinal();
				inicio[b] = bloque.inicioEnMinutos();
				fin[b] = bloque.finEnMinutos();
				for (FranjaDelDia franja : FranjaDelDia.values()) {
					porFranja[franja.ordinal()] += franja.minutosQueCaenEn(inicio[b], fin[b]);
				}
			}
			return new GrupoPlano(dia, inicio, fin, porFranja);
		}
	}

	/**
	 * Mide una combinación. Reutiliza sus arreglos entre llamadas, así que una instancia no se comparte entre
	 * hilos: cada {@link #proponer} crea la suya.
	 */
	private static final class Medidor {

		private final GrupoPlano[][] planos;
		private final int[][] inicios;
		private final int[][] fines;
		private final int[] cuenta = new int[DiaDeLaSemana.values().length];
		/** Los días en que alguna materia de la oferta tiene algún grupo con clase (SCRUM-251). */
		private final boolean[] diaDeLaOferta = new boolean[DiaDeLaSemana.values().length];

		Medidor(List<AsignaturaOfertada> oferta) {
			planos = new GrupoPlano[oferta.size()][];
			int bloquesPorDia = 0;
			for (int i = 0; i < oferta.size(); i++) {
				List<GrupoOfertado> grupos = oferta.get(i).grupos();
				planos[i] = new GrupoPlano[grupos.size()];
				int masBloques = 0;
				for (int g = 0; g < grupos.size(); g++) {
					planos[i][g] = GrupoPlano.de(grupos.get(g));
					masBloques = Math.max(masBloques, planos[i][g].dia().length);
					for (int dia : planos[i][g].dia()) {
						diaDeLaOferta[dia] = true;
					}
				}
				bloquesPorDia += masBloques;
			}
			inicios = new int[cuenta.length][bloquesPorDia];
			fines = new int[cuenta.length][bloquesPorDia];
		}

		Medida medir(int[] elegido) {
			int[] porFranja = new int[FranjaDelDia.values().length];
			boolean[] conClase = new boolean[cuenta.length];
			int[] porDia = new int[cuenta.length];
			Arrays.fill(cuenta, 0);
			for (int i = 0; i < elegido.length; i++) {
				GrupoPlano grupo = planos[i][elegido[i]];
				for (int f = 0; f < porFranja.length; f++) {
					porFranja[f] += grupo.minutosPorFranja()[f];
				}
				for (int b = 0; b < grupo.dia().length; b++) {
					int dia = grupo.dia()[b];
					inicios[dia][cuenta[dia]] = grupo.inicio()[b];
					fines[dia][cuenta[dia]] = grupo.fin()[b];
					porDia[dia] += grupo.fin()[b] - grupo.inicio()[b];
					cuenta[dia]++;
				}
			}
			int huecos = 0;
			for (int dia = 0; dia < cuenta.length; dia++) {
				if (cuenta[dia] > 0) {
					conClase[dia] = true;
					huecos += huecosDelDia(inicios[dia], fines[dia], cuenta[dia]);
				}
			}
			return new Medida(porFranja, huecos, conClase, porDia);
		}

		int cantidadDeDiasDeLaOferta() {
			int dias = 0;
			for (boolean usado : diaDeLaOferta) {
				dias += usado ? 1 : 0;
			}
			return dias;
		}

		/** Ordena los bloques del día por hora de inicio (son pocos) y suma el tiempo libre entre uno y el siguiente. */
		private static int huecosDelDia(int[] inicio, int[] fin, int n) {
			for (int i = 1; i < n; i++) {
				int inicioActual = inicio[i];
				int finActual = fin[i];
				int j = i - 1;
				while (j >= 0 && inicio[j] > inicioActual) {
					inicio[j + 1] = inicio[j];
					fin[j + 1] = fin[j];
					j--;
				}
				inicio[j + 1] = inicioActual;
				fin[j + 1] = finActual;
			}
			int huecos = 0;
			for (int i = 1; i < n; i++) {
				huecos += Math.max(0, inicio[i] - fin[i - 1]);
			}
			return huecos;
		}
	}

	/** De 0 a 1: el promedio, ponderado por los pesos, de los criterios que el estudiante pidió. */
	private static double puntuar(Medida medida, CriterioDeAcomodo criterio, int diasDeLaOferta) {
		double suma = 0;
		double pesos = 0;
		if (criterio.franja() != null) {
			int total = medida.total();
			suma += PESO_DE_LA_FRANJA * (total == 0 ? 0 : (double) medida.minutosPorFranja()[criterio.franja().ordinal()] / total);
			pesos += PESO_DE_LA_FRANJA;
		}
		if (criterio.pocosHuecos()) {
			suma += PESO_DE_LOS_HUECOS * Math.max(0.0, 1.0 - (double) medida.huecos() / HUECOS_QUE_ANULAN_EL_PUNTAJE);
			pesos += PESO_DE_LOS_HUECOS;
		}
		if (criterio.diaLibre()) {
			// Con «quiero un día libre» los días libres que se pidieron por separado se ignoran (SCRUM-251).
			suma += PESO_DEL_EQUILIBRIO * equilibrio(medida, diasDeLaOferta);
			pesos += PESO_DEL_EQUILIBRIO;
		} else if (!criterio.diasLibres().isEmpty()) {
			long libres = criterio.diasLibres().stream().filter(d -> !medida.diaConClase()[d.ordinal()]).count();
			suma += PESO_DE_LOS_DIAS_LIBRES * ((double) libres / criterio.diasLibres().size());
			pesos += PESO_DE_LOS_DIAS_LIBRES;
		}
		return pesos == 0 ? 1.0 : suma / pesos;
	}

	/**
	 * De 0 a 1, qué tan repartidas quedan las horas si se deja un día libre: lo que pesaría cada día con clase si
	 * todo se repartiera por igual entre los días de la oferta menos uno, dividido entre lo que pesa el más cargado.
	 * Todo en un solo día de cuatro vale 1/3; repartido en tres días vale 1.
	 */
	private static double equilibrio(Medida medida, int diasDeLaOferta) {
		if (diasDeLaOferta <= 1) {
			return 1.0;
		}
		int total = 0;
		int masCargado = 0;
		for (int minutos : medida.minutosPorDia()) {
			total += minutos;
			masCargado = Math.max(masCargado, minutos);
		}
		return masCargado == 0 ? 1.0 : Math.min(1.0, ((double) total / (diasDeLaOferta - 1)) / masCargado);
	}

	private PropuestaDeHorario propuesta(List<AsignaturaOfertada> oferta, CriterioDeAcomodo criterio, Candidato candidato, Medidor medidor) {
		Medida medida = medidor.medir(candidato.elegido());
		List<PropuestaDeHorario.Eleccion> elecciones = new ArrayList<>();
		for (int i = 0; i < candidato.elegido().length; i++) {
			GrupoOfertado grupo = oferta.get(i).grupos().get(candidato.elegido()[i]);
			elecciones.add(new PropuestaDeHorario.Eleccion(oferta.get(i).nombre(), grupo.codigo(), grupo.bloques()));
		}
		List<DiaDeLaSemana> conClase = new ArrayList<>();
		List<DiaDeLaSemana> libres = new ArrayList<>();
		for (int i = 0; i < DIAS_DE_CLASE; i++) {
			(medida.diaConClase()[i] ? conClase : libres).add(DiaDeLaSemana.values()[i]);
		}
		if (medida.diaConClase()[DiaDeLaSemana.DOMINGO.ordinal()]) {
			conClase.add(DiaDeLaSemana.DOMINGO);
		}
		List<DiaDeLaSemana> liberados = new ArrayList<>();
		for (DiaDeLaSemana dia : DiaDeLaSemana.values()) {
			if (medidor.diaDeLaOferta[dia.ordinal()] && !medida.diaConClase()[dia.ordinal()]) {
				liberados.add(dia);
			}
		}
		PropuestaDeHorario.Resumen resumen = new PropuestaDeHorario.Resumen(
				medida.minutosPorFranja()[FranjaDelDia.MANANA.ordinal()],
				medida.minutosPorFranja()[FranjaDelDia.TARDE.ordinal()],
				medida.minutosPorFranja()[FranjaDelDia.NOCHE.ordinal()],
				medida.huecos(), conClase, libres, liberados);
		return new PropuestaDeHorario(
				(int) Math.round(100 * candidato.puntaje()), elecciones, resumen,
				redactor.motivos(oferta.size(), criterio, resumen, diaMasCargado(medida)));
	}

	/** El día de la semana con más minutos de clase; con empate, el primero. */
	private static RedactorDeAcomodo.DiaMasCargado diaMasCargado(Medida medida) {
		int masCargado = 0;
		for (int dia = 1; dia < medida.minutosPorDia().length; dia++) {
			if (medida.minutosPorDia()[dia] > medida.minutosPorDia()[masCargado]) {
				masCargado = dia;
			}
		}
		return new RedactorDeAcomodo.DiaMasCargado(DiaDeLaSemana.values()[masCargado], medida.minutosPorDia()[masCargado]);
	}

	// --- Búsqueda --------------------------------------------------------------------------------------------

	private record Candidato(double puntaje, int[] elegido) {
	}

	private final class Busqueda {

		private final List<AsignaturaOfertada> oferta;
		private final CriterioDeAcomodo criterio;
		private final int[] elegido;
		private final Medidor medidor;
		private final List<Candidato> mejores = new ArrayList<>(MAXIMO_DE_PROPUESTAS + 1);
		/**
		 * Con «quiero un día libre»: la mejor combinación hallada para cada conjunto de días libres. La clave es el
		 * conjunto como máscara de bits (un bit por día de la semana), y el orden de un {@link TreeMap} es el de la semana.
		 */
		private final Map<Integer, Candidato> mejorPorConjuntoDeDiasLibres = new TreeMap<>();
		private long combinacionesValidas;
		private long pasos;
		private boolean incompleta;

		Busqueda(List<AsignaturaOfertada> oferta, CriterioDeAcomodo criterio) {
			this.oferta = oferta;
			this.criterio = criterio;
			this.elegido = new int[oferta.size()];
			this.medidor = new Medidor(oferta);
		}

		void recorrer(int asignatura) {
			if (asignatura == elegido.length) {
				considerar();
				return;
			}
			List<GrupoOfertado> grupos = oferta.get(asignatura).grupos();
			for (int g = 0; g < grupos.size() && !incompleta; g++) {
				if (++pasos > maximoDePasos) {
					incompleta = true;
					return;
				}
				if (!chocaConLosElegidos(asignatura, grupos.get(g))) {
					elegido[asignatura] = g;
					recorrer(asignatura + 1);
				}
			}
		}

		/**
		 * Guarda la combinación si deja algún día de la oferta sin clase —sin importar cuáles ni cuántos— y es mejor que
		 * la que ya había para ese mismo conjunto de días libres (con empate, se queda la hallada antes).
		 */
		private void guardarSiEsLaMejorParaSuConjuntoDeDiasLibres(Medida medida, double puntaje) {
			int conjunto = 0;
			for (int dia = 0; dia < medidor.diaDeLaOferta.length; dia++) {
				if (medidor.diaDeLaOferta[dia] && !medida.diaConClase()[dia]) {
					conjunto |= 1 << dia;
				}
			}
			Candidato actual = mejorPorConjuntoDeDiasLibres.get(conjunto);
			if (conjunto != 0 && (actual == null || puntaje > actual.puntaje())) {
				mejorPorConjuntoDeDiasLibres.put(conjunto, new Candidato(puntaje, elegido.clone()));
			}
		}

		/**
		 * Una opción por cada conjunto distinto de días libres, de la mejor repartida a la peor; con empate, la que libera
		 * más días y, con empate, la del conjunto que va primero en la semana. Una combinación tiene un solo conjunto, así
		 * que aparece una sola vez. Se devuelven como máximo {@link #MAXIMO_DE_OPCIONES_CON_DIA_LIBRE}.
		 */
		List<Candidato> opcionesConDiaLibre() {
			return mejorPorConjuntoDeDiasLibres.entrySet().stream()
					.sorted(Comparator.<Map.Entry<Integer, Candidato>>comparingDouble(e -> e.getValue().puntaje())
							.reversed()
							.thenComparing(e -> -Integer.bitCount(e.getKey())))
					.limit(MAXIMO_DE_OPCIONES_CON_DIA_LIBRE)
					.map(Map.Entry::getValue)
					.toList();
		}

		private boolean chocaConLosElegidos(int hasta, GrupoOfertado grupo) {
			for (int previa = 0; previa < hasta; previa++) {
				if (oferta.get(previa).grupos().get(elegido[previa]).chocaCon(grupo)) {
					return true;
				}
			}
			return false;
		}

		/** Se queda con la combinación solo si entra entre las mejores; con puntaje igual, la que se halló antes va primero. */
		private void considerar() {
			Medida medida = medidor.medir(elegido);
			combinacionesValidas++;
			double puntaje = puntuar(medida, criterio, medidor.cantidadDeDiasDeLaOferta());
			if (criterio.diaLibre()) {
				guardarSiEsLaMejorParaSuConjuntoDeDiasLibres(medida, puntaje);
				return;
			}
			if (mejores.size() == MAXIMO_DE_PROPUESTAS && puntaje <= mejores.getLast().puntaje()) {
				return;
			}
			int posicion = mejores.size();
			for (int i = 0; i < mejores.size(); i++) {
				if (mejores.get(i).puntaje() < puntaje) {
					posicion = i;
					break;
				}
			}
			mejores.add(posicion, new Candidato(puntaje, elegido.clone()));
			if (mejores.size() > MAXIMO_DE_PROPUESTAS) {
				mejores.removeLast();
			}
		}
	}
}
