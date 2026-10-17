package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Duration;
import java.time.Instant;

/**
 * Algo que el estudiante tiene que entregar o presentar, con fecha (RF06, SCRUM-29): una tarea, un
 * parcial, un taller. Es suyo y de nadie más; asociarlo a una asignatura es opcional.
 *
 * @param id nulo mientras no se haya guardado
 * @param idMatricula la asignatura a la que pertenece; nulo si no está asociado a ninguna
 * @param descripcion nula si no escribió nada
 */
public record Pendiente(
		Integer id,
		int idEstudiante,
		Integer idMatricula,
		String titulo,
		String descripcion,
		Instant fechaLimite,
		PrioridadDePendiente prioridad,
		EstadoDePendiente estado,
		Instant fechaCreacion) {

	/** Hasta cuánto antes del vencimiento se resalta como urgente (SCRUM-30). */
	public static final Duration UMBRAL_DE_URGENCIA = Duration.ofHours(48);

	/** Lo que caben en las columnas titulo y descripcion de la tabla pendiente. */
	static final int LARGO_DEL_TITULO = 120;
	static final int LARGO_DE_LA_DESCRIPCION = 300;

	public Pendiente {
		titulo = titulo == null ? "" : titulo.strip();
		if (titulo.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Escribe un título para el pendiente");
		}
		if (titulo.length() > LARGO_DEL_TITULO) {
			throw new ReglaDeNegocioVioladaException(
					"El título no puede pasar de %d caracteres".formatted(LARGO_DEL_TITULO));
		}
		descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
		if (descripcion != null && descripcion.length() > LARGO_DE_LA_DESCRIPCION) {
			throw new ReglaDeNegocioVioladaException(
					"La descripción no puede pasar de %d caracteres".formatted(LARGO_DE_LA_DESCRIPCION));
		}
		if (fechaLimite == null) {
			throw new ReglaDeNegocioVioladaException("Falta la fecha límite del pendiente");
		}
		if (prioridad == null) {
			prioridad = PrioridadDePendiente.MEDIA;
		}
		if (estado == null || fechaCreacion == null) {
			throw new IllegalArgumentException("Un pendiente siempre tiene estado y fecha de creación");
		}
	}

	/** Un pendiente recién escrito: abierto, sin número y con prioridad media si no eligió otra. */
	public static Pendiente nuevo(
			int idEstudiante,
			Integer idMatricula,
			String titulo,
			String descripcion,
			Instant fechaLimite,
			PrioridadDePendiente prioridad,
			Instant ahora) {
		return new Pendiente(null, idEstudiante, idMatricula, titulo, descripcion, fechaLimite, prioridad,
				EstadoDePendiente.ABIERTO, ahora);
	}

	public Pendiente cumplir() {
		return conEstado(EstadoDePendiente.CUMPLIDO);
	}

	public Pendiente reabrir() {
		return conEstado(EstadoDePendiente.ABIERTO);
	}

	/**
	 * Cumplido; vencido si se pasó la fecha límite (la hora exacta también cuenta); urgente si vence en
	 * menos de 48 horas; y si no, próximo.
	 */
	public SituacionDelPendiente situacionA(Instant ahora) {
		if (estado == EstadoDePendiente.CUMPLIDO) {
			return SituacionDelPendiente.CUMPLIDO;
		}
		if (!fechaLimite.isAfter(ahora)) {
			return SituacionDelPendiente.VENCIDO;
		}
		return Duration.between(ahora, fechaLimite).compareTo(UMBRAL_DE_URGENCIA) < 0
				? SituacionDelPendiente.URGENTE
				: SituacionDelPendiente.PROXIMO;
	}

	private Pendiente conEstado(EstadoDePendiente nuevo) {
		return new Pendiente(id, idEstudiante, idMatricula, titulo, descripcion, fechaLimite, prioridad, nuevo,
				fechaCreacion);
	}
}
