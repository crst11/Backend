package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.Docente;
import co.edu.ucundinamarca.cundiapp.domain.model.EspacioFisico;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteDeHorarioDetectado;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Convierte las palabras de un Reporte de Horario, con su posición en la página, en las asignaturas
 * y las clases que el estudiante va a confirmar (RF04, SCRUM-81).
 *
 * <p>Trabaja sobre palabras con coordenadas y no sobre el texto corrido porque el horario es una
 * tabla: las clases de cada día están en columnas, y en el texto corrido una hora de martes y una de
 * miércoles quedan en el mismo renglón sin forma de saber cuál es cuál. El día solo lo da la
 * columna en la que cae cada hora.
 *
 * <p>El reporte trae tres listados: los docentes (con su documento), los salones y el horario. Las
 * clases dicen el salón y el documento del docente, y de esos listados salen el bloque, la sede y el
 * nombre. **El documento de un docente solo sirve para esa búsqueda y no sale de aquí** (Ley 1581
 * de 2012); tampoco se lee nunca el nombre ni el documento del estudiante, que van en el encabezado.
 */
@Component
class LectorDeReporteDeHorario {

	private static final String MENSAJE_NO_ES_EL_REPORTE =
			"El archivo no parece un Reporte de Horario de la universidad. Sube el reporte que dice "
					+ "«Horario - Campo de aprendizaje»; el Registro Académico Extendido se importa desde Mi historial";

	/** Una celda termina donde hay un hueco así de grande entre dos palabras. */
	private static final double HUECO_ENTRE_CELDAS = 20;
	/** Dos palabras con tan poca diferencia vertical están en el mismo renglón. */
	private static final double TOLERANCIA_DE_RENGLON = 3;
	/** El código de la asignatura va unos puntos por encima de la hora de su primera clase. */
	private static final double MARGEN_DEL_CODIGO = 4;
	/** Lo que mide hacia abajo la celda de una clase: la hora, el salón y el documento del docente. */
	private static final double ALTO_DE_LA_CELDA = 30;
	/** Hasta dónde se busca el grupo debajo del código, si no hay otra asignatura que lo limite. */
	private static final double ALTO_DE_LA_ASIGNATURA = 60;
	/** Cuánto a la izquierda del centro de la primera columna empieza la columna de los códigos. */
	private static final double MARGEN_IZQUIERDO_DE_LAS_COLUMNAS = 38;
	/** La mitad del ancho de una columna de día, como mucho. */
	private static final double MEDIA_COLUMNA = 35;

	private static final Pattern FECHA = Pattern.compile("\\d{2}-\\d{2}-\\d{4}");
	private static final DateTimeFormatter FORMATO_DE_FECHA = DateTimeFormatter.ofPattern("dd-MM-uuuu");
	private static final Pattern CODIGO_DE_ASIGNATURA = Pattern.compile("^(?=.*\\d)[A-Z]{2,4}-?[A-Z0-9]{8,16}$");
	private static final Pattern HORAS = Pattern.compile("^(\\d{2}:\\d{2})-(\\d{2}:\\d{2})$");
	private static final Pattern SOLO_DIGITOS = Pattern.compile("^\\d{5,}$");
	/** El año de la ruta que separa el programa de la jornada o la sede: "… COMPUTACION 2020 - MIXTA". */
	private static final Pattern ANIO_DE_LA_RUTA = Pattern.compile("\\s\\d{4}\\s*-\\s*");

	private static final Map<String, DiaDeLaSemana> DIAS = Map.of(
			"lunes", DiaDeLaSemana.LUNES,
			"martes", DiaDeLaSemana.MARTES,
			"miercoles", DiaDeLaSemana.MIERCOLES,
			"jueves", DiaDeLaSemana.JUEVES,
			"viernes", DiaDeLaSemana.VIERNES,
			"sabado", DiaDeLaSemana.SABADO,
			"domingo", DiaDeLaSemana.DOMINGO);

	public ReporteDeHorarioDetectado leer(List<Palabra> palabras) {
		List<Palabra> ordenadas = palabras == null
				? List.of()
				: palabras.stream()
						.sorted(Comparator.comparingDouble(Palabra::arriba).thenComparingDouble(Palabra::x0))
						.toList();
		List<List<Palabra>> renglones = renglones(ordenadas);

		int encabezadoDelHorario = renglonQueEmpieza(renglones, 0, "Horario - Campo de aprendizaje");
		if (encabezadoDelHorario < 0) {
			throw new ReglaDeNegocioVioladaException(MENSAJE_NO_ES_EL_REPORTE);
		}

		LocalDate fecha = fechaDelReporte(renglones);
		String[] programaYSede = programaYSede(renglones);
		Map<String, Docente> docentes = docentes(renglones);
		Map<String, EspacioFisico> espacios = espacios(renglones);

		List<AsignaturaConHorario> asignaturas = horario(
				ordenadas, renglones.get(encabezadoDelHorario).getFirst().arriba(), docentes, espacios, programaYSede[1]);
		if (asignaturas.isEmpty()) {
			throw new ReglaDeNegocioVioladaException(
					"El Reporte de Horario no trae ninguna asignatura. Revisa que sea el reporte de tu horario");
		}
		return new ReporteDeHorarioDetectado(programaYSede[0], programaYSede[1], fecha, asignaturas);
	}

	// ------------------------------------------------------------------ encabezado

	private static LocalDate fechaDelReporte(List<List<Palabra>> renglones) {
		int renglon = renglonQueEmpieza(renglones, 0, "Fecha del Reporte");
		if (renglon >= 0) {
			for (Palabra palabra : renglones.get(renglon)) {
				Matcher fecha = FECHA.matcher(palabra.texto());
				if (fecha.matches()) {
					try {
						return LocalDate.parse(palabra.texto(), FORMATO_DE_FECHA);
					} catch (DateTimeParseException error) {
						break;
					}
				}
			}
		}
		throw new ReglaDeNegocioVioladaException(
				"No encontramos la fecha del reporte, que es la que dice a qué semestre corresponde el horario");
	}

	/**
	 * El renglón que sigue a «Programa» trae, de izquierda a derecha, el programa con su sede, el
	 * nombre del estudiante y su documento. Solo se lee la primera celda.
	 */
	private static String[] programaYSede(List<List<Palabra>> renglones) {
		int renglon = renglonQueEmpieza(renglones, 0, "Programa");
		if (renglon < 0 || renglon + 1 >= renglones.size()) {
			return new String[] {"", ""};
		}
		List<String> celdas = celdas(renglones.get(renglon + 1));
		String primera = celdas.isEmpty() ? "" : celdas.getFirst();
		Matcher anio = ANIO_DE_LA_RUTA.matcher(primera);
		return anio.find() && anio.end() < primera.length()
				? new String[] {primera.substring(0, anio.start()).strip(), primera.substring(anio.end()).strip()}
				: new String[] {primera.strip(), ""};
	}

	// ------------------------------------------------------------------ listados

	/** Documento y nombre de cada docente. El documento es solo la llave de la búsqueda. */
	private static Map<String, Docente> docentes(List<List<Palabra>> renglones) {
		Map<String, Docente> docentes = new HashMap<>();
		int inicio = renglonQueEmpieza(renglones, 0, "Identificación");
		if (inicio < 0) {
			return docentes;
		}
		for (int i = inicio + 1; i < renglones.size() && !empiezaCon(renglones.get(i), "Listado"); i++) {
			List<String> celdas = celdas(renglones.get(i));
			if (celdas.size() >= 2 && SOLO_DIGITOS.matcher(celdas.get(0)).matches()) {
				docenteDe(celdas.get(1)).ifPresent(docente -> docentes.put(celdas.get(0), docente));
			}
		}
		return docentes;
	}

	/** Salón, nombre del bloque y localidad de cada espacio físico. */
	private static Map<String, EspacioFisico> espacios(List<List<Palabra>> renglones) {
		Map<String, EspacioFisico> espacios = new HashMap<>();
		int inicio = renglonQueEmpieza(renglones, 0, "Nomenclatura");
		if (inicio < 0) {
			return espacios;
		}
		for (int i = inicio + 1; i < renglones.size() && !empiezaCon(renglones.get(i), "Horario"); i++) {
			List<String> celdas = celdas(renglones.get(i));
			if (celdas.size() >= 3) {
				espacios.put(celdas.get(0), new EspacioFisico(celdas.get(0), celdas.get(1), celdas.get(2)));
			}
		}
		return espacios;
	}

	/**
	 * El reporte da el nombre completo en un solo texto. Los dos últimos nombres son los apellidos y lo
	 * demás los nombres; es solo cómo se muestra, porque el docente se identifica por el nombre
	 * completo normalizado.
	 */
	private static Optional<Docente> docenteDe(String completo) {
		String[] partes = completo.strip().split("\\s+");
		if (partes.length < 2) {
			return Optional.empty();
		}
		int primerApellido = partes.length >= 3 ? partes.length - 2 : 1;
		String nombres = String.join(" ", java.util.Arrays.copyOfRange(partes, 0, primerApellido));
		String apellidos = String.join(" ", java.util.Arrays.copyOfRange(partes, primerApellido, partes.length));
		return Optional.of(new Docente(conMayusculaInicial(nombres), conMayusculaInicial(apellidos)));
	}

	private static String conMayusculaInicial(String texto) {
		StringBuilder resultado = new StringBuilder();
		for (String palabra : texto.split("\\s+")) {
			if (!resultado.isEmpty()) {
				resultado.append(' ');
			}
			resultado.append(palabra.substring(0, 1).toUpperCase(Locale.ROOT))
					.append(palabra.substring(1).toLowerCase(Locale.ROOT));
		}
		return resultado.toString();
	}

	// ------------------------------------------------------------------ el horario

	private static List<AsignaturaConHorario> horario(
			List<Palabra> todas,
			double arribaDelEncabezado,
			Map<String, Docente> docentes,
			Map<String, EspacioFisico> espacios,
			String sedeDelReporte) {
		List<Palabra> delHorario = todas.stream().filter(p -> p.arriba() > arribaDelEncabezado).toList();

		Map<DiaDeLaSemana, Double> columnas = columnasDeLosDias(delHorario);
		if (columnas.isEmpty()) {
			throw new ReglaDeNegocioVioladaException(MENSAJE_NO_ES_EL_REPORTE);
		}
		double limiteIzquierdo = columnas.values().stream().mapToDouble(Double::doubleValue).min().orElseThrow()
				- MARGEN_IZQUIERDO_DE_LAS_COLUMNAS;

		List<Palabra> codigos = delHorario.stream()
				.filter(p -> p.x0() < limiteIzquierdo && CODIGO_DE_ASIGNATURA.matcher(p.texto()).matches())
				.toList();
		List<AsignaturaConHorario> asignaturas = new ArrayList<>();
		for (int i = 0; i < codigos.size(); i++) {
			Palabra codigo = codigos.get(i);
			double hasta = i + 1 < codigos.size() ? codigos.get(i + 1).arriba() : Double.MAX_VALUE;
			List<BloqueDeHorario> bloques = bloquesDe(
					delHorario, codigo.arriba(), hasta, limiteIzquierdo, columnas, docentes, espacios, sedeDelReporte);
			asignaturas.add(new AsignaturaConHorario(
					codigo.texto(), grupoDe(delHorario, codigo.arriba(), hasta, limiteIzquierdo), bloques));
		}
		return asignaturas;
	}

	/** El centro de cada columna es el centro del nombre del día en el encabezado de la tabla. */
	private static Map<DiaDeLaSemana, Double> columnasDeLosDias(List<Palabra> delHorario) {
		Map<DiaDeLaSemana, Double> columnas = new HashMap<>();
		for (Palabra palabra : delHorario) {
			DiaDeLaSemana dia = DIAS.get(sinTildes(palabra.texto()).toLowerCase(Locale.ROOT));
			if (dia != null && !columnas.containsKey(dia)) {
				columnas.put(dia, palabra.centro());
			}
		}
		return columnas;
	}

	/**
	 * Cada hora (`09:00-10:59`) es una clase. Su día lo da la columna donde cae, y debajo de ella, en
	 * la misma columna, vienen el salón y el documento del docente.
	 */
	private static List<BloqueDeHorario> bloquesDe(
			List<Palabra> delHorario,
			double desde,
			double hasta,
			double limiteIzquierdo,
			Map<DiaDeLaSemana, Double> columnas,
			Map<String, Docente> docentes,
			Map<String, EspacioFisico> espacios,
			String sedeDelReporte) {
		List<Palabra> enLaAsignatura = delHorario.stream()
				.filter(p -> p.x0() >= limiteIzquierdo && p.arriba() >= desde - MARGEN_DEL_CODIGO && p.arriba() < hasta)
				.toList();

		record Hallada(DiaDeLaSemana dia, LocalTime inicio, LocalTime fin, EspacioFisico espacio, Docente docente) {
		}
		Set<Hallada> halladas = new LinkedHashSet<>();
		for (Palabra hora : enLaAsignatura) {
			Matcher horas = HORAS.matcher(hora.texto());
			DiaDeLaSemana dia = diaDe(hora, columnas);
			if (!horas.matches() || dia == null) {
				continue;
			}
			double siguienteEnLaColumna = enLaAsignatura.stream()
					.filter(p -> HORAS.matcher(p.texto()).matches() && diaDe(p, columnas) == dia && p.arriba() > hora.arriba())
					.mapToDouble(Palabra::arriba)
					.min()
					.orElse(Double.MAX_VALUE);
			List<String> celda = enLaAsignatura.stream()
					.filter(p -> diaDe(p, columnas) == dia
							&& p.arriba() > hora.arriba()
							&& p.arriba() <= hora.arriba() + ALTO_DE_LA_CELDA
							&& p.arriba() < siguienteEnLaColumna)
					.map(Palabra::texto)
					.toList();
			halladas.add(new Hallada(
					dia,
					horaDe(horas.group(1)),
					horaDe(horas.group(2)),
					espacioDe(celda, espacios, sedeDelReporte),
					docenteDe(celda, docentes)));
		}

		List<Hallada> ordenadas = halladas.stream()
				.sorted(Comparator.comparing(Hallada::dia).thenComparing(Hallada::inicio))
				.toList();
		List<BloqueDeHorario> bloques = new ArrayList<>();
		for (int i = 0; i < ordenadas.size(); i++) {
			Hallada h = ordenadas.get(i);
			bloques.add(BloqueDeHorario.delReporte(i + 1, h.dia(), h.inicio(), h.fin(), h.espacio(), h.docente()));
		}
		return bloques;
	}

	/** El día de una palabra, o nulo si cae a la izquierda de las columnas o en medio de ninguna. */
	private static DiaDeLaSemana diaDe(Palabra palabra, Map<DiaDeLaSemana, Double> columnas) {
		DiaDeLaSemana masCercano = null;
		double distancia = Double.MAX_VALUE;
		for (var columna : columnas.entrySet()) {
			double d = Math.abs(palabra.centro() - columna.getValue());
			if (d < distancia) {
				distancia = d;
				masCercano = columna.getKey();
			}
		}
		return distancia <= MEDIA_COLUMNA ? masCercano : null;
	}

	/** El primer texto de la celda que no es un documento es el salón. */
	private static EspacioFisico espacioDe(
			List<String> celda, Map<String, EspacioFisico> espacios, String sedeDelReporte) {
		for (String texto : celda) {
			if (SOLO_DIGITOS.matcher(texto).matches() || HORAS.matcher(texto).matches() || esMarca(texto)) {
				continue;
			}
			EspacioFisico conocido = espacios.get(texto);
			if (conocido != null) {
				return conocido;
			}
			return sedeDelReporte == null || sedeDelReporte.isBlank() ? null : new EspacioFisico(texto, null, sedeDelReporte);
		}
		return null;
	}

	private static Docente docenteDe(List<String> celda, Map<String, Docente> docentes) {
		return celda.stream()
				.filter(texto -> SOLO_DIGITOS.matcher(texto).matches())
				.map(docentes::get)
				.filter(java.util.Objects::nonNull)
				.findFirst()
				.orElse(null);
	}

	/** «RE» y «DO» son las marcas del reporte para «sin recurso físico» y «sin docente». */
	private static boolean esMarca(String texto) {
		return texto.equals("RE") || texto.equals("DO");
	}

	/** El grupo va a la izquierda, después de «Grupo :», y puede seguir en el renglón de abajo. */
	private static String grupoDe(List<Palabra> delHorario, double desde, double hasta, double limiteIzquierdo) {
		List<String> izquierda = delHorario.stream()
				.filter(p -> p.x0() < limiteIzquierdo
						&& p.arriba() >= desde
						&& p.arriba() < hasta
						&& p.arriba() < desde + ALTO_DE_LA_ASIGNATURA)
				.sorted(Comparator.comparingDouble(Palabra::arriba).thenComparingDouble(Palabra::x0))
				.map(Palabra::texto)
				.toList();
		int indice = izquierda.indexOf("Grupo");
		if (indice < 0) {
			return null;
		}
		return izquierda.subList(indice + 1, izquierda.size()).stream()
				.map(texto -> texto.replaceFirst("^:+", ""))
				.filter(texto -> !texto.isBlank())
				.reduce((a, b) -> a + " " + b)
				.orElse(null);
	}

	private static LocalTime horaDe(String texto) {
		try {
			return LocalTime.parse(texto);
		} catch (DateTimeParseException error) {
			throw new ReglaDeNegocioVioladaException("No pudimos leer la hora «" + texto + "» del horario");
		}
	}

	// ------------------------------------------------------------------ renglones y celdas

	private static List<List<Palabra>> renglones(List<Palabra> ordenadas) {
		List<List<Palabra>> renglones = new ArrayList<>();
		List<Palabra> actual = null;
		double arribaDelActual = 0;
		for (Palabra palabra : ordenadas) {
			if (actual == null || palabra.arriba() - arribaDelActual > TOLERANCIA_DE_RENGLON) {
				actual = new ArrayList<>();
				renglones.add(actual);
				arribaDelActual = palabra.arriba();
			}
			actual.add(palabra);
		}
		renglones.replaceAll(r -> r.stream().sorted(Comparator.comparingDouble(Palabra::x0)).toList());
		return renglones;
	}

	/** Las palabras de un renglón agrupadas en celdas: un hueco grande separa una celda de la otra. */
	private static List<String> celdas(List<Palabra> renglon) {
		List<String> celdas = new ArrayList<>();
		StringBuilder celda = new StringBuilder();
		Palabra anterior = null;
		for (Palabra palabra : renglon) {
			if (anterior != null && palabra.x0() - anterior.x1() > HUECO_ENTRE_CELDAS) {
				celdas.add(celda.toString());
				celda.setLength(0);
			}
			if (!celda.isEmpty()) {
				celda.append(' ');
			}
			celda.append(palabra.texto());
			anterior = palabra;
		}
		if (!celda.isEmpty()) {
			celdas.add(celda.toString());
		}
		return celdas;
	}

	private static int renglonQueEmpieza(List<List<Palabra>> renglones, int desde, String texto) {
		for (int i = desde; i < renglones.size(); i++) {
			if (empiezaCon(renglones.get(i), texto)) {
				return i;
			}
		}
		return -1;
	}

	private static boolean empiezaCon(List<Palabra> renglon, String texto) {
		String completo = renglon.stream().map(Palabra::texto).reduce((a, b) -> a + " " + b).orElse("");
		return completo.startsWith(texto);
	}

	private static String sinTildes(String texto) {
		return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
	}
}
