package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Una asignatura que el estudiante quiere matricular, con los grupos entre los que puede elegir (RF13, SCRUM-207). */
public record AsignaturaOfertada(String nombre, List<GrupoOfertado> grupos) {

	public static final int MAXIMO_DE_GRUPOS = 12;
	public static final int MAXIMO_DEL_NOMBRE = 80;

	public AsignaturaOfertada {
		if (nombre == null || nombre.isBlank()) {
			throw new ReglaDeNegocioVioladaException("Falta el nombre de la asignatura");
		}
		nombre = nombre.strip();
		if (nombre.length() > MAXIMO_DEL_NOMBRE) {
			throw new ReglaDeNegocioVioladaException(
					"El nombre de la asignatura no puede pasar de %d caracteres".formatted(MAXIMO_DEL_NOMBRE));
		}
		if (grupos == null || grupos.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("La asignatura «%s» necesita al menos un grupo".formatted(nombre));
		}
		if (grupos.size() > MAXIMO_DE_GRUPOS) {
			throw new ReglaDeNegocioVioladaException(
					"La asignatura «%s» no puede tener más de %d grupos".formatted(nombre, MAXIMO_DE_GRUPOS));
		}
		grupos = List.copyOf(grupos);
		Set<String> codigos = new HashSet<>();
		for (GrupoOfertado grupo : grupos) {
			if (!codigos.add(grupo.codigo().toLowerCase(Locale.ROOT))) {
				throw new ReglaDeNegocioVioladaException(
						"El grupo «%s» se repite en «%s»: cada grupo se escribe una sola vez".formatted(grupo.codigo(), nombre));
			}
		}
	}
}
