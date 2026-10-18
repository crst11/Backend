package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import co.edu.ucundinamarca.cundiapp.domain.model.SuscripcionPush;
import java.time.Instant;
import java.util.List;

/** Lo que el núcleo necesita para guardar las suscripciones Web Push de los dispositivos (RF10, SCRUM-37). */
public interface DispositivoConAvisosRepositorio {

	/** Guarda la suscripción; si el estudiante ya tenía una con la misma dirección, la actualiza y la reactiva. */
	DispositivoConAvisos guardarSuscripcion(int idEstudiante, String nombre, SuscripcionPush suscripcion, Instant ahora);

	/** Los dispositivos del estudiante que hoy pueden recibir un aviso: con suscripción y activos. */
	List<DispositivoConAvisos> activos(int idEstudiante);

	/** La suscripción ya no existe en el navegador: se borra y el dispositivo deja de recibir avisos. */
	void desactivar(int idEstudiante, int consecutivo);

	/**
	 * La persona quita el dispositivo de sus avisos: la fila se borra. Falso si no existe, es de otro estudiante
	 * o ya no recibía avisos: la respuesta no deja averiguar qué dispositivos existen.
	 */
	boolean quitar(int idEstudiante, int consecutivo);
}
