package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Qué clase de día es cada fecha: lectivo, festivo o fuera del período (RF07, SCRUM-85).
 *
 * <p>Los semestres usan las fechas aproximadas de {@link PeriodoAcademico} (el primero de febrero a
 * fin de junio y el segundo de agosto a mediados de diciembre). Cuando se cargue el calendario
 * oficial de la universidad, con su receso y sus parciales, se reemplazan aquí y nadie más cambia.
 */
public class CalendarioAcademico {

	public DiaDelCalendario clasificar(LocalDate fecha) {
		Optional<String> festivo = FestivosDeColombia.nombreDe(fecha);
		if (festivo.isPresent()) {
			return new DiaDelCalendario(fecha, TipoDeDia.FESTIVO, festivo.get());
		}
		return new DiaDelCalendario(fecha, dentroDeUnPeriodo(fecha) ? TipoDeDia.LECTIVO : TipoDeDia.FUERA_DE_PERIODO, null);
	}

	/** Un día por fecha, de `desde` a `hasta` incluidas. */
	public List<DiaDelCalendario> entre(LocalDate desde, LocalDate hasta) {
		List<DiaDelCalendario> dias = new ArrayList<>();
		for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
			dias.add(clasificar(fecha));
		}
		return dias;
	}

	/** El primer día lectivo estrictamente posterior a la fecha. */
	public LocalDate siguienteDiaLectivoDespuesDe(LocalDate fecha) {
		LocalDate candidato = fecha.plusDays(1);
		while (!clasificar(candidato).tieneClase()) {
			candidato = candidato.plusDays(1);
		}
		return candidato;
	}

	private static boolean dentroDeUnPeriodo(LocalDate fecha) {
		for (int semestre = 1; semestre <= 2; semestre++) {
			var periodo = new PeriodoAcademico(fecha.getYear(), semestre);
			if (!fecha.isBefore(periodo.inicioAproximado()) && !fecha.isAfter(periodo.finAproximado())) {
				return true;
			}
		}
		return false;
	}
}
