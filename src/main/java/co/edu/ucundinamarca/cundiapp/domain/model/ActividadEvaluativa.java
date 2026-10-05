package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Una actividad calificable dentro de una categoría: el parcial, el taller, el quiz (RF05).
 *
 * <p>El porcentaje es el peso <em>dentro de su categoría</em>, no dentro de la asignatura. Un
 * parcial que vale 70 % del primer corte, y el corte 30 % de la materia, pesa 21 % en la nota
 * final; esa multiplicación la hace el simulador, no esta clase.
 *
 * <p>La fecha puede venir nula: muchas actividades existen en el plan del docente antes de tener
 * día asignado, y obligar a inventarse una fecha llenaría la agenda del estudiante de mentiras.
 */
public record ActividadEvaluativa(
		int consecutivo,
		String nombre,
		BigDecimal porcentaje,
		LocalDate fechaProgramada,
		TipoDeActividad tipo,
		EstadoDeEntrega estado) {

	/** Lo que cabe en actividad_evaluativa.nombre_actividad. */
	static final int LARGO_DEL_NOMBRE = 120;

	public ActividadEvaluativa {
		ReglasDeLaEstructura.consecutivoValido(consecutivo, "la actividad");
		ReglasDeLaEstructura.nombreValido(nombre, LARGO_DEL_NOMBRE, "la actividad");
		ReglasDeLaEstructura.porcentajeValido(porcentaje, "la actividad «%s»".formatted(nombre));
		if (tipo == null) {
			throw new ReglaDeNegocioVioladaException(
					"La actividad «%s» necesita un tipo".formatted(nombre));
		}
		if (estado == null) {
			throw new ReglaDeNegocioVioladaException(
					"La actividad «%s» necesita un estado de entrega".formatted(nombre));
		}
	}

	public Optional<LocalDate> fecha() {
		return Optional.ofNullable(fechaProgramada);
	}
}
