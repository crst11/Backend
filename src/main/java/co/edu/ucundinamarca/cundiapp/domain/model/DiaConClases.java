package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/** Las clases de un día, en orden de hora (RF04, SCRUM-25). */
public record DiaConClases(DiaDeLaSemana dia, List<ClaseDelHorario> clases) {

	public DiaConClases {
		clases = List.copyOf(clases);
	}
}
