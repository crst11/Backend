package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Cómo se ordenan las clases dentro de un día (RF04, SCRUM-26). Refleja los valores de la restricción
 * ck_configuracion_horario_orden del esquema.
 */
public enum OrdenDelHorario {
	/** Por hora de inicio: lo que se consulta en el momento. Es el orden por defecto. */
	HORA("hora"),
	/** Por nombre de asignatura y, dentro de cada una, por hora: ver todo lo de una materia junto. */
	ASIGNATURA("asignatura");

	private final String valorEnBd;

	OrdenDelHorario(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	/** También lo manda la pantalla en la consulta, así que un valor malo es un 422 y no un error. */
	public static OrdenDelHorario desdeBd(String valor) {
		for (OrdenDelHorario orden : values()) {
			if (orden.valorEnBd.equals(valor)) {
				return orden;
			}
		}
		throw new ReglaDeNegocioVioladaException(
				"El orden «%s» no es válido: usa hora o asignatura".formatted(valor));
	}
}
