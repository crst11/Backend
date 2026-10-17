package co.edu.ucundinamarca.cundiapp.domain.model;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * El riesgo de una asignatura con su motivo y la proyección en la que se apoya (RF09, SCRUM-35).
 * La proyección es nula solo cuando no hay datos: sin estructura de evaluación no hay cuenta que hacer.
 */
public record RiesgoDeAsignatura(NivelDeRiesgo nivel, MotivoDelRiesgo motivo, ProyeccionDeNota proyeccion) {

	/**
	 * De lo más urgente a lo menos: primero el nivel y, dentro de él, la nota más difícil de sacar
	 * (la inalcanzable, que no tiene nota, va delante de todas).
	 */
	public static final Comparator<RiesgoDeAsignatura> PRIORIDAD = Comparator
			.comparing(RiesgoDeAsignatura::nivel)
			.thenComparing(RiesgoDeAsignatura::notaRequerida,
					Comparator.nullsFirst(Comparator.<BigDecimal>reverseOrder()));

	public static RiesgoDeAsignatura sinDatos() {
		return new RiesgoDeAsignatura(NivelDeRiesgo.SIN_DATOS, MotivoDelRiesgo.SIN_ESTRUCTURA, null);
	}

	/** La nota que hace falta, o nula si no hay datos o la meta ya no se alcanza. */
	public BigDecimal notaRequerida() {
		return proyeccion == null ? null : proyeccion.notaRequerida();
	}
}
