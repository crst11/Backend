package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PendienteConSituacion;
import co.edu.ucundinamarca.cundiapp.domain.model.PrioridadDePendiente;
import java.time.Instant;

/**
 * Escribir un pendiente: título, fecha límite y prioridad, y opcionalmente la asignatura (RF06, SCRUM-29).
 * Devuelve el pendiente ya con su número y cómo está ahora, para que la pantalla lo ponga en su lugar.
 */
public interface CrearMiPendiente {

	PendienteConSituacion ejecutar(int idEstudiante, Datos datos);

	/**
	 * @param idMatricula nulo si no lo asocia a una asignatura
	 * @param prioridad nula si no eligió: queda en media
	 */
	record Datos(
			String titulo, String descripcion, Instant fechaLimite, PrioridadDePendiente prioridad, Integer idMatricula) {
	}
}
