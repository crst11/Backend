package co.edu.ucundinamarca.cundiapp.application.port.in;

/**
 * Crea los avisos de los pendientes que están por vencer (RF10, SCRUM-37). Lo llama la tarea
 * programada cada pocos minutos, no un estudiante.
 *
 * <p>Un pendiente abierto genera un aviso cuando le falta menos que la anticipación de su dueño
 * (24 horas por defecto) y este tiene los avisos activos. **Una sola vez**: aunque la tarea corra mil
 * veces, el mismo pendiente no vuelve a avisar.
 */
public interface GenerarAvisosDeVencimiento {

	/** @return cuántos avisos nuevos se crearon en esta ronda */
	int ejecutar();
}
