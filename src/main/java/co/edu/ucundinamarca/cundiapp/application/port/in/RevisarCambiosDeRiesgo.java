package co.edu.ucundinamarca.cundiapp.application.port.in;

/**
 * Mira si el riesgo de una asignatura cambió de nivel y, si cambió, avisa (RF10, SCRUM-50). Lo llaman los
 * casos de uso que pueden moverlo: anotar o quitar una nota, cambiar la meta, los umbrales o la estructura de
 * evaluación.
 *
 * <p>Nunca debe tumbar la acción que lo dispara: la nota queda guardada aunque el aviso no se pueda crear.
 */
public interface RevisarCambiosDeRiesgo {

	/** Revisa una sola asignatura (una nota o su estructura cambió). */
	void deLaAsignatura(int idEstudiante, int idMatricula);

	/** Revisa todas las que cursa (la meta o los umbrales cambiaron: afectan a todas). */
	void deTodasLasAsignaturas(int idEstudiante);
}
