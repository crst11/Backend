package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PendienteConSituacion;

/** Marcar un pendiente como hecho, o volver a abrirlo si se marcó por error (RF06, SCRUM-29). */
public interface CambiarEstadoDeMiPendiente {

	PendienteConSituacion ejecutar(int idEstudiante, int idPendiente, boolean cumplido);
}
