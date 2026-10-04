package co.edu.ucundinamarca.cundiapp.application.port.in;

/**
 * Devuelve el historial del estudiante al estado en que estaba antes de una carga (RF03, SCRUM-24).
 *
 * <p>Deshacer no es borrar: lo que la carga creó se elimina, pero lo que pisó vuelve al valor que
 * tenía. Por eso cada importación guarda un respaldo de lo que toca.
 */
public interface DeshacerImportacion {

	Resultado ejecutar(int idEstudiante, int idImportacion);

	/** Lo que se devolvió, para contárselo al estudiante con números y no con un "listo". */
	record Resultado(int idImportacion, int eliminadas, int restauradas) {
	}
}
