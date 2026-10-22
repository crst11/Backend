package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.model.CriterioDeAcomodo;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.PropuestaDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeAcomodo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Escribe lo que dice el motor de acomodo (RF13, SCRUM-207). El texto sale del dominio y no de la pantalla,
 * como en los avisos, para que diga lo mismo venga de donde venga.
 */
class RedactorDeAcomodo {

	/** Las frases que explican por qué se propone una combinación, en el orden de lo que el estudiante pidió. */
	List<String> motivos(
			int asignaturas, CriterioDeAcomodo criterio, PropuestaDeHorario.Resumen resumen, DiaMasCargado masCargado) {
		List<String> motivos = new ArrayList<>();
		motivos.add(asignaturas == 1 ? "Una sola materia, sin choques." : "Sin choques entre tus %d materias.".formatted(asignaturas));
		if (criterio.franja() != null) {
			int minutos = resumen.minutosEn(criterio.franja());
			int total = resumen.minutosDeClase();
			long porcentaje = total == 0 ? 0 : Math.round(100.0 * minutos / total);
			motivos.add(minutos == 0
					? "Ninguna de tus clases queda en %s.".formatted(criterio.franja().nombre())
					: "El %d %% de tus horas de clase queda en %s.".formatted(porcentaje, criterio.franja().nombre()));
		}
		if (criterio.pocosHuecos()) {
			motivos.add(resumen.minutosDeHuecos() == 0
					? "No tienes huecos entre clases."
					: "Tienes %s libres entre clases en toda la semana.".formatted(duracion(resumen.minutosDeHuecos())));
		}
		if (criterio.diaLibre()) {
			motivos.add("Te quedan libres: %s.".formatted(lista(resumen.diasLiberados())));
			motivos.add("Tu día más cargado es el %s, con %s.".formatted(nombre(masCargado.dia()), duracion(masCargado.minutos())));
		} else if (!criterio.diasLibres().isEmpty()) {
			motivos.add(diasLibres(criterio, resumen));
		}
		return motivos;
	}

	/** El día de la semana con más clase y cuánto pesa (SCRUM-251): lo que dicen los motivos de «quiero un día libre». */
	record DiaMasCargado(DiaDeLaSemana dia, int minutos) {
	}

	String mensajeDeTopeSinDiaLibre() {
		return "La búsqueda se cortó antes de hallar un día que se pueda dejar libre, y puede que sí exista. "
				+ "Prueba con menos materias o menos grupos.";
	}

	String mensajeSinDiaLibre() {
		return "Con estas materias no se puede dejar ningún día libre: todas las combinaciones tienen clase en cada día "
				+ "que ofrecen. Quita la marca «Quiero tener un día libre» o cambia una materia.";
	}

	private static String diasLibres(CriterioDeAcomodo criterio, PropuestaDeHorario.Resumen resumen) {
		List<DiaDeLaSemana> logrados = criterio.diasLibres().stream().filter(d -> !resumen.diasConClase().contains(d)).toList();
		List<DiaDeLaSemana> fallidos = criterio.diasLibres().stream().filter(resumen.diasConClase()::contains).toList();
		if (fallidos.isEmpty()) {
			return "Te quedan libres: %s.".formatted(lista(logrados));
		}
		if (logrados.isEmpty()) {
			return "No se logró dejar libre: %s.".formatted(lista(fallidos));
		}
		return "Te quedan libres: %s; no se logró dejar libre: %s.".formatted(lista(logrados), lista(fallidos));
	}

	String mensajeDeChoques(List<ResultadoDeAcomodo.Choque> choques) {
		String parejas = choques.stream()
				.map(c -> "«%s» y «%s»: todos sus grupos chocan".formatted(c.asignatura(), c.otra()))
				.collect(Collectors.joining("; "));
		return "No hay combinación sin choques. " + parejas + ". Quita una de cada pareja o busca otro grupo.";
	}

	String mensajeGeneral() {
		return "Ninguna combinación de grupos evita los choques entre tus materias. Prueba quitando una materia o agregando otro grupo.";
	}

	String mensajeDeTope() {
		return "No se encontró una combinación en el tiempo de búsqueda. Prueba con menos materias o menos grupos.";
	}

	/** «45 min», «2 h» o «2 h 30 min». */
	static String duracion(int minutos) {
		int horas = minutos / 60;
		int resto = minutos % 60;
		if (horas == 0) {
			return resto + " min";
		}
		return resto == 0 ? horas + " h" : horas + " h " + resto + " min";
	}

	/** «lunes», «lunes y viernes» o «lunes, martes y viernes». */
	static String lista(List<DiaDeLaSemana> dias) {
		List<String> nombres = dias.stream().map(RedactorDeAcomodo::nombre).toList();
		if (nombres.size() <= 1) {
			return String.join("", nombres);
		}
		return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.getLast();
	}

	private static String nombre(DiaDeLaSemana dia) {
		return switch (dia) {
			case LUNES -> "lunes";
			case MARTES -> "martes";
			case MIERCOLES -> "miércoles";
			case JUEVES -> "jueves";
			case VIERNES -> "viernes";
			case SABADO -> "sábado";
			case DOMINGO -> "domingo";
		};
	}
}
