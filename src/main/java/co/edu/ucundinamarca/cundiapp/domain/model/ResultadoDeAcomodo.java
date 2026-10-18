package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.List;

/**
 * Lo que devuelve el motor de acomodo (RF13, SCRUM-207): hasta tres propuestas o, si no hay ninguna, la
 * explicación de por qué.
 *
 * @param choques las parejas de asignaturas cuyos grupos chocan todos entre sí; vacío si hay propuestas o si
 *     el problema no es de una pareja
 * @param mensaje por qué no hay propuestas; nulo si las hay
 * @param busquedaIncompleta verdadero si se llegó al tope de la búsqueda: puede haber combinaciones mejores
 */
public record ResultadoDeAcomodo(
		List<PropuestaDeHorario> propuestas, List<Choque> choques, String mensaje, boolean busquedaIncompleta) {

	public ResultadoDeAcomodo {
		propuestas = List.copyOf(propuestas);
		choques = List.copyOf(choques);
	}

	/** Dos asignaturas cuyos grupos chocan todos entre sí. */
	public record Choque(String asignatura, String otra) {
	}
}
