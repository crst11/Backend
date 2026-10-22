package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * El grupo que el estudiante tiene elegido de una materia en su horario armado (RF13, SCRUM-245). La app no lo
 * guarda: llega en la petición cada vez que se verifica un cambio de grupo.
 */
public record EleccionDeHorario(String asignatura, GrupoOfertado grupo) {

	public EleccionDeHorario {
		if (asignatura == null || asignatura.isBlank()) {
			throw new ReglaDeNegocioVioladaException("Falta el nombre de una de las materias del horario");
		}
		if (grupo == null) {
			throw new ReglaDeNegocioVioladaException("Falta el grupo de la materia «%s»".formatted(asignatura.strip()));
		}
		asignatura = asignatura.strip();
	}
}
