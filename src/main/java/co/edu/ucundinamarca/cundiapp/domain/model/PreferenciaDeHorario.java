package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Cómo le gusta al estudiante ver su horario, y lo que se guarda para la próxima vez (RF04, SCRUM-26):
 * con qué vista abre, de qué sede, y en qué orden.
 *
 * <p>El filtro por asignatura <em>no</em> se guarda a propósito: cambia cada período, y una
 * preferencia atada a una materia que ya no se cursa dejaría el horario vacío sin explicación.
 *
 * @param sede nula si quiere ver todas
 */
public record PreferenciaDeHorario(VistaDelHorario vista, String sede, OrdenDelHorario orden) {

	/** Lo que ve quien nunca eligió nada: el día, todas las sedes y por hora. */
	public static final PreferenciaDeHorario PREDETERMINADA =
			new PreferenciaDeHorario(VistaDelHorario.DIA, null, OrdenDelHorario.HORA);

	/** Lo que cabe en configuracion_estudiante.horario_sede. */
	static final int LARGO_DE_LA_SEDE = 60;

	public PreferenciaDeHorario {
		if (vista == null) {
			throw new ReglaDeNegocioVioladaException("Falta la vista del horario");
		}
		if (orden == null) {
			throw new ReglaDeNegocioVioladaException("Falta el orden del horario");
		}
		sede = sede == null || sede.isBlank() ? null : sede.strip();
		if (sede != null && sede.length() > LARGO_DE_LA_SEDE) {
			throw new ReglaDeNegocioVioladaException(
					"La sede no puede pasar de %d caracteres".formatted(LARGO_DE_LA_SEDE));
		}
	}

	/** La sede guardada filtra el horario; la asignatura nunca, porque no se guarda. */
	public FiltroDeHorario comoFiltro() {
		return new FiltroDeHorario(null, sede);
	}
}
