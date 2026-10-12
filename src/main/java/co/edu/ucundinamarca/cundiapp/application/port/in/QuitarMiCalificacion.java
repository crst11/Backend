package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;

/**
 * Quitar la nota de una actividad (RF05, SCRUM-28). La actividad vuelve a quedar sin entregar y el
 * acumulado se recalcula sin ella.
 */
public interface QuitarMiCalificacion {

	NotaAcumulada ejecutar(int idEstudiante, int idMatricula, int consecCategoria, int consecActividad);
}
