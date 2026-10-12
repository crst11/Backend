package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * El horario de la semana del estudiante: todas sus clases (RF04, SCRUM-25).
 *
 * <p>Cómo se lee un horario, por día y en orden de hora, es una regla de dominio y no un asunto de la
 * pantalla: la vista por día y la vista por semana piden lo mismo y no vuelven a ordenar nada.
 * Dos clases a la misma hora se muestran las dos; avisar de un choque es otra historia.
 */
public record HorarioSemanal(List<ClaseDelHorario> clases) {

	private static final Comparator<ClaseDelHorario> POR_HORA = Comparator
			.<ClaseDelHorario, java.time.LocalTime>comparing(clase -> clase.bloque().horaInicio())
			.thenComparing(clase -> clase.bloque().horaFin())
			.thenComparing(ClaseDelHorario::asignatura);

	public HorarioSemanal {
		clases = List.copyOf(clases);
	}

	public boolean estaVacio() {
		return clases.isEmpty();
	}

	public int totalDeClases() {
		return clases.size();
	}

	/** Las clases de un día, en orden de hora. Vacía, y no un error, si ese día no hay clases. */
	public List<ClaseDelHorario> delDia(DiaDeLaSemana dia) {
		return clases.stream().filter(clase -> clase.bloque().dia() == dia).sorted(POR_HORA).toList();
	}

	/** Solo los días que tienen clases, de lunes a domingo. */
	public List<DiaConClases> dias() {
		return Arrays.stream(DiaDeLaSemana.values())
				.map(dia -> new DiaConClases(dia, delDia(dia)))
				.filter(dia -> !dia.clases().isEmpty())
				.toList();
	}
}
