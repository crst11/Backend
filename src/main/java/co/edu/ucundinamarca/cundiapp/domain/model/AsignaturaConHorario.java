package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.List;

/**
 * Una asignatura tal como la lista el Reporte de Horario: su código, su grupo y las clases de la
 * semana (RF04, SCRUM-81). El nombre no se lee del reporte, porque sale partido en varios renglones
 * y la ruta de aprendizaje ya lo tiene.
 */
public record AsignaturaConHorario(String codigo, String grupo, List<BloqueDeHorario> bloques) {

	/** Lo que cabe en matricula_asignatura.codigo_grupo. */
	static final int LARGO_DEL_GRUPO = 20;

	public AsignaturaConHorario {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("Falta el código de la asignatura");
		}
		codigo = codigo.strip();
		grupo = grupo == null || grupo.isBlank() ? null : grupo.strip();
		if (grupo != null && grupo.length() > LARGO_DEL_GRUPO) {
			throw new ReglaDeNegocioVioladaException(
					"El grupo no puede pasar de %d caracteres".formatted(LARGO_DEL_GRUPO));
		}
		bloques = bloques == null ? List.of() : List.copyOf(bloques);
	}
}
