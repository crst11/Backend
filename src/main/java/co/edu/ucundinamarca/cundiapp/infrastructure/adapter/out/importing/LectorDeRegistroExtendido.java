package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.importing;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaDetectada;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoDetectado;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteAcademicoDetectado;
import co.edu.ucundinamarca.cundiapp.domain.model.ResumenOficialDePeriodo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Convierte el texto de un Registro Académico Extendido en lo que el estudiante va a confirmar.
 *
 * <p>Trabaja sobre texto y no sobre el PDF a propósito: así se prueba con un reporte de ejemplo
 * escrito a mano y ningún archivo con datos de nadie tiene que entrar al repositorio.
 *
 * <p>Del reporte solo se sacan tres cosas: el programa, los totales que la universidad publica por
 * período, y de cada asignatura su código y su nota definitiva. El nombre y los créditos no se
 * leen: al pasar el PDF a texto las columnas se entreveran (un nombre largo se parte en varios
 * renglones y se mezcla con el grupo), y además esos datos ya están en la ruta de aprendizaje
 * cargada en la base, que es la fuente oficial.
 */
@Component
public class LectorDeRegistroExtendido {

	/** Lo que identifica a este reporte y no a otro de Academusoft. */
	private static final String TITULO = "Consultar Registro Académico Extendido";

	/** Ejemplo: "2024 - 2 16 16 4.4 4.4" */
	private static final Pattern ENCABEZADO_DE_PERIODO =
			Pattern.compile("^(\\d{4})\\s*-\\s*(\\d)\\s+(\\d+)\\s+(\\d+)\\s+([\\d.]+)\\s+([\\d.]+)$");

	/** Ejemplo: "CAD612021101 ÁLGEBRA LINEAL NORMAL 3 F.103M 4,2 4,2" */
	private static final Pattern FILA_DE_ASIGNATURA =
			Pattern.compile("^([A-Z]{2,4}-?[A-Z0-9]{10,20})\\s+.+?\\s+(NORMAL|HOMOLOGADA|REPETIDA|VALIDADA)\\s+(\\d+)\\s+(.*)$");

	/** Las notas van con coma: "4,2". El último número de la fila es la definitiva. */
	private static final Pattern NOTA = Pattern.compile("\\b([0-5],\\d)\\b");

	private static final String LINEA_DEL_PROGRAMA = "Programa Jornada Ruta de Aprendizaje";

	public ReporteAcademicoDetectado leer(String texto) {
		if (texto == null || !texto.contains(TITULO)) {
			throw new ReglaDeNegocioVioladaException(
					"El archivo no parece un Registro Académico Extendido de la universidad");
		}

		List<String> lineas = texto.lines().map(String::trim).toList();
		List<PeriodoDetectado> periodos = new ArrayList<>();
		List<ResumenOficialDePeriodo> oficiales = new ArrayList<>();

		String periodoActual = null;
		List<AsignaturaDetectada> asignaturas = new ArrayList<>();

		for (String linea : lineas) {
			Matcher encabezado = ENCABEZADO_DE_PERIODO.matcher(linea);
			if (encabezado.matches()) {
				guardar(periodos, periodoActual, asignaturas);
				periodoActual = encabezado.group(1) + "-" + encabezado.group(2);
				asignaturas = new ArrayList<>();
				oficiales.add(new ResumenOficialDePeriodo(
						periodoActual,
						Integer.parseInt(encabezado.group(3)),
						Integer.parseInt(encabezado.group(4)),
						new BigDecimal(encabezado.group(5)),
						new BigDecimal(encabezado.group(6))));
				continue;
			}

			Matcher fila = FILA_DE_ASIGNATURA.matcher(linea);
			if (periodoActual != null && fila.matches()) {
				asignaturas.add(new AsignaturaDetectada(fila.group(1), ultimaNotaDe(fila.group(4))));
			}
			// Cualquier otro renglón es ruido del PDF: continuaciones de nombre o de grupo.
		}
		guardar(periodos, periodoActual, asignaturas);

		return new ReporteAcademicoDetectado(programaDe(lineas), sedeDe(lineas), periodos, oficiales);
	}

	private static void guardar(
			List<PeriodoDetectado> periodos, String codigo, List<AsignaturaDetectada> asignaturas) {
		if (codigo != null && !asignaturas.isEmpty()) {
			periodos.add(new PeriodoDetectado(codigo, asignaturas));
		}
	}

	/**
	 * La última nota de la fila es la definitiva. Cuando hubo habilitación el reporte imprime tres
	 * (final, habilitación y definitiva) y la que cuenta es la última, no la primera.
	 */
	private static BigDecimal ultimaNotaDe(String resto) {
		Matcher notas = NOTA.matcher(resto);
		String ultima = null;
		while (notas.find()) {
			ultima = notas.group(1);
		}
		return ultima == null ? null : new BigDecimal(ultima.replace(',', '.'));
	}

	/** El nombre del programa va antes del año de la ruta: "... COMPUTACION 2020 - MIXTA". */
	private static String programaDe(List<String> lineas) {
		return despuesDe(lineas, LINEA_DEL_PROGRAMA)
				.map(linea -> linea.split("\\s+\\d{4}\\s*-")[0].trim())
				.orElse("");
	}

	/** La sede abre el renglón siguiente al del programa. */
	private static String sedeDe(List<String> lineas) {
		int indice = lineas.indexOf(lineas.stream()
				.filter(linea -> linea.startsWith(LINEA_DEL_PROGRAMA))
				.findFirst()
				.orElse(""));
		if (indice < 0 || indice + 2 >= lineas.size()) {
			return "";
		}
		return lineas.get(indice + 2).split("\\s+")[0];
	}

	private static java.util.Optional<String> despuesDe(List<String> lineas, String encabezado) {
		for (int i = 0; i < lineas.size() - 1; i++) {
			if (lineas.get(i).startsWith(encabezado)) {
				return java.util.Optional.of(lineas.get(i + 1));
			}
		}
		return java.util.Optional.empty();
	}
}
