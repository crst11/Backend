package co.edu.ucundinamarca.cundiapp.application.port.in;

/**
 * Entrega a los dispositivos los avisos que todavía no salieron (RF10, SCRUM-37). Lo llama la tarea
 * programada.
 *
 * <p>La entrega por Web Push es un extra: el aviso ya se ve dentro de la aplicación desde que se crea.
 * Si el estudiante no tiene dispositivos con avisos, o el servidor no tiene llaves VAPID, el aviso queda
 * como fallido en su entrega y sigue visible dentro de la aplicación (criterio de SCRUM-37).
 */
public interface EntregarAvisosPendientes {

	/** @return cuántos avisos llegaron al menos a un dispositivo en esta ronda */
	int ejecutar();
}
