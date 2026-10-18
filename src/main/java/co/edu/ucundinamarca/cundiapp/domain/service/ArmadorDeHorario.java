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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
	static final int MAXIMO_DE_PASOS = 2_000_000;
	/** Con estos minutos libres entre clases en la semana, el puntaje de «pocos huecos» llega a cero. */
	static final int HUECOS_QUE_ANULAN_EL_PUNTAJE = 600;

	private static final int PESO_DE_LA_FRANJA = 60;
	private static final int PESO_DE_LOS_HUECOS = 25;
	private static final int PESO_DE_LOS_DIAS_LIBRES = 15;
	private static final int DIAS_DE_CLASE = 6;

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
		if (busqueda.mejores.isEmpty()) {
			return sinSolucion(oferta, busqueda.incompleta);
		}
		List<PropuestaDeHorario> propuestas = busqueda.mejores.stream()
				.map(candidato -> propuesta(oferta, pedido, candidato))
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
	private record Medida(int[] minutosPorFranja, int huecos, boolean[] diaConClase) {

		int total() {
			int suma = 0;
			for (int minutos : minutosPorFranja) {
				suma += minutos;
			}
			return suma;
		}
	}

	private static Medida medir(List<AsignaturaOfertada> oferta, int[] elegido) {
		List<BloqueDeOferta> bloques = new ArrayList<>();
		for (int i = 0; i < elegido.length; i++) {
			bloques.addAll(oferta.get(i).grupos().get(elegido[i]).bloques());
		}
		bloques.sort(Comparator.comparing(BloqueDeOferta::dia).thenComparing(BloqueDeOferta::inicio));
		int[] porFranja = new int[FranjaDelDia.values().length];
		boolean[] conClase = new boolean[DiaDeLaSemana.values().length];
		int huecos = 0;
		BloqueDeOferta anterior = null;
		for (BloqueDeOferta bloque : bloques) {
			for (FranjaDelDia franja : FranjaDelDia.values()) {
				porFranja[franja.ordinal()] += franja.minutosQueCaenEn(bloque.inicioEnMinutos(), bloque.finEnMinutos());
			}
			conClase[bloque.dia().ordinal()] = true;
			if (anterior != null && anterior.dia() == bloque.dia()) {
				huecos += Math.max(0, bloque.inicioEnMinutos() - anterior.finEnMinutos());
			}
			anterior = bloque;
		}
		return new Medida(porFranja, huecos, conClase);
	}

	/** De 0 a 1: el promedio, ponderado por los pesos, de los criterios que el estudiante pidió. */
	private static double puntuar(Medida medida, CriterioDeAcomodo criterio) {
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
		if (!criterio.diasLibres().isEmpty()) {
			long libres = criterio.diasLibres().stream().filter(d -> !medida.diaConClase()[d.ordinal()]).count();
			suma += PESO_DE_LOS_DIAS_LIBRES * ((double) libres / criterio.diasLibres().size());
			pesos += PESO_DE_LOS_DIAS_LIBRES;
		}
		return pesos == 0 ? 1.0 : suma / pesos;
	}

	private PropuestaDeHorario propuesta(List<AsignaturaOfertada> oferta, CriterioDeAcomodo criterio, Candidato candidato) {
		Medida medida = medir(oferta, candidato.elegido());
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
		PropuestaDeHorario.Resumen resumen = new PropuestaDeHorario.Resumen(
				medida.minutosPorFranja()[FranjaDelDia.MANANA.ordinal()],
				medida.minutosPorFranja()[FranjaDelDia.TARDE.ordinal()],
				medida.minutosPorFranja()[FranjaDelDia.NOCHE.ordinal()],
				medida.huecos(), conClase, libres);
		return new PropuestaDeHorario(
				(int) Math.round(100 * candidato.puntaje()), elecciones, resumen, redactor.motivos(oferta.size(), criterio, resumen));
	}

	// --- Búsqueda --------------------------------------------------------------------------------------------

	private record Candidato(double puntaje, int[] elegido) {
	}

	private final class Busqueda {

		private final List<AsignaturaOfertada> oferta;
		private final CriterioDeAcomodo criterio;
		private final int[] elegido;
		private final List<Candidato> mejores = new ArrayList<>(MAXIMO_DE_PROPUESTAS + 1);
		private long pasos;
		private boolean incompleta;

		Busqueda(List<AsignaturaOfertada> oferta, CriterioDeAcomodo criterio) {
			this.oferta = oferta;
			this.criterio = criterio;
			this.elegido = new int[oferta.size()];
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
			double puntaje = puntuar(medir(oferta, elegido), criterio);
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
