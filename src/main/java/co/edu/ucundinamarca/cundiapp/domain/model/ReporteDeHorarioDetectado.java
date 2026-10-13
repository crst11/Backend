package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que se lee del Reporte de Horario de la universidad (RF04, SCRUM-81): el programa, la sede, la
 * fecha en que se imprimió y las asignaturas con sus clases.
 *
 * <p>Del reporte no se lee nada de identidad: ni el nombre del estudiante ni su documento, ni los
 * documentos de los docentes (Ley 1581 de 2012). El documento de un docente solo sirve para saber
 * a quién pertenece cada clase dentro del archivo y no sale de la lectura.
 */
public record ReporteDeHorarioDetectado(
		String programa, String sede, LocalDate fecha, List<AsignaturaConHorario> asignaturas) {

	public ReporteDeHorarioDetectado {
		asignaturas = asignaturas == null ? List.of() : List.copyOf(asignaturas);
	}

	/** El período al que corresponde, deducido de la fecha del reporte. */
	public PeriodoAcademico periodo() {
		return PeriodoAcademico.delaFecha(fecha);
	}
}
