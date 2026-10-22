package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/**
 * Lo que pasa si la persona cambia una materia por este grupo (RF13, SCRUM-245): {@code cabe} cuando no choca
 * con ninguna otra materia del horario; si no, {@code choques} dice con cuáles.
 */
public record ResultadoDelCandidato(String grupo, boolean cabe, List<ChoqueDelGrupo> choques) {

	public ResultadoDelCandidato {
		choques = List.copyOf(choques);
	}
}
