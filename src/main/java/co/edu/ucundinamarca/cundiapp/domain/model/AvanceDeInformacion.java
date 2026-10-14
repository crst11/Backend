package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Cuánta de su información lleva cargada el estudiante y qué le falta (RF03, SCRUM-84).
 *
 * <p>Se mide por pasos hechos sobre pasos totales. Los pasos son el Registro Académico Extendido, el
 * Reporte de Horario y **uno por cada asignatura que cursa ahora**, que se cumple cuando ya tiene sus
 * actividades cargadas. Cada semestre cambia la lista de asignaturas y el porcentaje se recalcula solo.
 *
 * <p>El porcentaje se trunca: 2 de 3 pasos son 66 % y no 67 %, para no decir «casi listo» de más.
 */
public record AvanceDeInformacion(List<PasoDeAvance> pasos) {

	public AvanceDeInformacion {
		pasos = List.copyOf(pasos);
	}

	public static AvanceDeInformacion de(
			boolean tieneRegistroExtendido, boolean tieneHorario, List<AsignaturaMatriculada> asignaturas) {
		List<PasoDeAvance> pasos = new ArrayList<>();
		pasos.add(new PasoDeAvance(TipoDePaso.REGISTRO_EXTENDIDO, "Registro Académico Extendido", tieneRegistroExtendido, null));
		pasos.add(new PasoDeAvance(TipoDePaso.HORARIO, "Reporte de Horario", tieneHorario, null));
		for (AsignaturaMatriculada asignatura : asignaturas) {
			if (asignatura.estaEnCurso()) {
				pasos.add(new PasoDeAvance(
						TipoDePaso.ASIGNATURA,
						"Actividades de " + asignatura.nombre(),
						asignatura.actividadesDefinidas() > 0,
						asignatura.idMatricula()));
			}
		}
		return new AvanceDeInformacion(pasos);
	}

	public int total() {
		return pasos.size();
	}

	public int hechos() {
		return (int) pasos.stream().filter(PasoDeAvance::hecho).count();
	}

	public int porcentaje() {
		return total() == 0 ? 0 : hechos() * 100 / total();
	}
}
