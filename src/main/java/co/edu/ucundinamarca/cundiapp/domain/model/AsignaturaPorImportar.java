package co.edu.ucundinamarca.cundiapp.domain.model;

import java.math.BigDecimal;

/**
 * Una asignatura detectada, ya cruzada con la ruta de aprendizaje, como se le muestra al
 * estudiante para que confirme (RF03, SCRUM-23).
 *
 * <p>`enElPlan` dice si el código aparece en la ruta de aprendizaje de su programa. Si no está, la
 * app no la inventa ni la descarta en silencio: se la muestra y le avisa que no se va a guardar,
 * porque sin la asignatura en el plan no hay nombre ni créditos con qué contarla.
 */
public record AsignaturaPorImportar(
		String codigo,
		String nombre,
		int creditos,
		BigDecimal notaDefinitiva,
		EstadoDeAsignatura estado,
		boolean enElPlan,
		boolean yaEstaba) {

	public boolean seVaAGuardar() {
		return enElPlan;
	}
}
