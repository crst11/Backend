package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.List;

/**
 * Un grupo de una asignatura, con los bloques en que se dicta (RF13, SCRUM-207). El código es el que el
 * estudiante ve al matricular («1», «A», «01»).
 */
public record GrupoOfertado(String codigo, List<BloqueDeOferta> bloques) {

	public static final int MAXIMO_DE_BLOQUES = 8;
	public static final int MAXIMO_DEL_CODIGO = 20;

	public GrupoOfertado {
		if (codigo == null || codigo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("Falta el código del grupo");
		}
		codigo = codigo.strip();
		if (codigo.length() > MAXIMO_DEL_CODIGO) {
			throw new ReglaDeNegocioVioladaException("El código del grupo no puede pasar de %d caracteres".formatted(MAXIMO_DEL_CODIGO));
		}
		if (bloques == null || bloques.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("El grupo «%s» necesita al menos un bloque (día y horas)".formatted(codigo));
		}
		if (bloques.size() > MAXIMO_DE_BLOQUES) {
			throw new ReglaDeNegocioVioladaException(
					"El grupo «%s» no puede tener más de %d bloques".formatted(codigo, MAXIMO_DE_BLOQUES));
		}
		bloques = List.copyOf(bloques);
		for (int i = 0; i < bloques.size(); i++) {
			for (int j = i + 1; j < bloques.size(); j++) {
				if (bloques.get(i).chocaCon(bloques.get(j))) {
					throw new ReglaDeNegocioVioladaException(
							"Dos bloques del grupo «%s» se solapan: revisa los días y las horas".formatted(codigo));
				}
			}
		}
	}

	/** Dos grupos chocan si algún bloque de uno choca con algún bloque del otro. */
	public boolean chocaCon(GrupoOfertado otro) {
		for (BloqueDeOferta propio : bloques) {
			for (BloqueDeOferta ajeno : otro.bloques) {
				if (propio.chocaCon(ajeno)) {
					return true;
				}
			}
		}
		return false;
	}
}
