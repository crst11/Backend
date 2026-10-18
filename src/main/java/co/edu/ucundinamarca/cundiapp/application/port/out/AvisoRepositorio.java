package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Lo que el núcleo necesita para guardar y leer los avisos (RF10, SCRUM-37). */
public interface AvisoRepositorio {

	/** Un pendiente que ya está dentro de la anticipación de su dueño y todavía no tiene su aviso de vencimiento. */
	record PendientePorAvisar(int idEstudiante, int idPendiente, String titulo, Instant fechaLimite) {
	}

	/**
	 * Los pendientes abiertos que vencen dentro de la anticipación de su dueño, de estudiantes con la cuenta
	 * activa y los avisos encendidos, que todavía no tienen aviso de vencimiento. Primero los que vencen antes.
	 */
	List<PendientePorAvisar> pendientesPorAvisar(Instant ahora);

	/**
	 * Guarda el aviso de vencimiento de un pendiente. Falso si ya existía: la base de datos no admite dos, así
	 * que dos rondas de la tarea, o dos instancias del servidor, no duplican el aviso.
	 */
	boolean crearDeVencimiento(Aviso aviso, int idPendiente);

	/**
	 * Los pendientes abiertos que se vencieron hace menos de {@code vigencia}, de estudiantes con la cuenta activa
	 * y los avisos encendidos, que todavía no tienen aviso de «se venció». Primero los que se vencieron antes. La
	 * vigencia evita avisar de pendientes viejos cuando la función se enciende por primera vez.
	 */
	List<PendientePorAvisar> pendientesVencidos(Instant ahora, Duration vigencia);

	/** Guarda el aviso de «se venció» de un pendiente. Falso si ya existía, igual que {@link #crearDeVencimiento}. */
	boolean crearDeVencido(Aviso aviso, int idPendiente);

	/** Guarda el aviso de que una asignatura cambió de nivel de riesgo (SCRUM-50). Cada cambio es un aviso. */
	void crearDeCambioDeRiesgo(Aviso aviso, int idMatricula);

	/** Los avisos que todavía no se entregaron, del más antiguo al más reciente. */
	List<Aviso> porEntregar(int limite);

	void marcarEnviado(int idAviso, Instant ahora);

	/** Suma un intento. Si es definitivo, o ya no quedan intentos, el aviso pasa a fallido; si no, sigue pendiente. */
	void registrarFallo(int idAviso, boolean definitivo);

	List<Aviso> deEstudiante(int idEstudiante, boolean soloSinLeer, int limite);

	int sinLeer(int idEstudiante);

	/** Falso si el aviso no existe o es de otro estudiante: la respuesta no deja averiguar cuáles existen. */
	boolean marcarLeido(int idEstudiante, int idAviso, boolean leido);

	void marcarTodosLeidos(int idEstudiante);
}
