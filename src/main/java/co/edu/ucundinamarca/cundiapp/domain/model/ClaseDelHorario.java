package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Un bloque del horario junto con aquello a lo que pertenece (RF04, SCRUM-25): lo que se ve en una fila de la
 * pantalla de horario y en la del día.
 *
 * <p>Pertenece a una asignatura que el estudiante cursa ({@code idMatricula}) o es una actividad extra
 * ({@code extra}, SCRUM-266), nunca las dos cosas. En una actividad extra {@code idMatricula} y
 * {@code codigoAsignatura} son nulos y {@code asignatura} lleva su nombre, para que se ordene y se muestre como
 * una clase más.
 */
public record ClaseDelHorario(
		Integer idMatricula, String codigoAsignatura, String asignatura, BloqueDeHorario bloque, ActividadExtra extra) {

	public ClaseDelHorario {
		if ((idMatricula == null) == (extra == null)) {
			throw new IllegalArgumentException("Una clase es de una asignatura o es una actividad extra, no las dos ni ninguna");
		}
	}

	/** Una clase de una asignatura: es lo que casi siempre se construye. */
	public ClaseDelHorario(int idMatricula, String codigoAsignatura, String asignatura, BloqueDeHorario bloque) {
		this(Integer.valueOf(idMatricula), codigoAsignatura, asignatura, bloque, null);
	}

	public static ClaseDelHorario deActividadExtra(ActividadExtra actividad) {
		return new ClaseDelHorario(null, null, actividad.nombre(), actividad.comoBloque(), actividad);
	}

	public boolean esExtra() {
		return extra != null;
	}
}
