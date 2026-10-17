package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;

/**
 * Activa los avisos en este dispositivo: guarda la suscripción Web Push que el navegador acaba de crear
 * (RF10, SCRUM-37). Si el dispositivo ya estaba suscrito con esa misma dirección, la actualiza en vez de
 * duplicarla.
 */
public interface RegistrarMiSuscripcionPush {

	/** @param nombre cómo se llama el dispositivo en la lista de la persona; vacío usa «Este dispositivo» */
	DispositivoConAvisos ejecutar(int idEstudiante, String nombre, SuscripcionPush suscripcion);
}
