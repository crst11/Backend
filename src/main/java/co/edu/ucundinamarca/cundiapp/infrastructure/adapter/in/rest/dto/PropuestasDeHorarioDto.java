package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeOferta;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.PropuestaDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeAcomodo;
import java.util.List;

/**
 * Las combinaciones que propone el motor de acomodo, con el texto ya escrito por el servidor: la pantalla
 * solo las muestra (RF13, SCRUM-208).
 *
 * @param propuestas de cero a tres, de la mejor a la peor
 * @param choques si no hay propuestas, las parejas de materias cuyos grupos chocan todos entre sí
 * @param mensaje si no hay propuestas, por qué; nulo si las hay
 * @param busquedaIncompleta si se llegó al tope de la búsqueda y puede haber combinaciones mejores
 */
public record PropuestasDeHorarioDto(
		List<PropuestaDto> propuestas, List<ChoqueDto> choques, String mensaje, boolean busquedaIncompleta) {

	public record PropuestaDto(int puntaje, List<EleccionDto> elecciones, ResumenDto resumen, List<String> motivos) {
	}

	public record EleccionDto(String asignatura, String grupo, List<BloqueDto> bloques) {
	}

	/** Un bloque del grupo elegido: día sin tildes y horas como {@code 08:00}. */
	public record BloqueDto(String dia, String inicio, String fin) {
	}

	public record ResumenDto(
			int minutosEnLaManana,
			int minutosEnLaTarde,
			int minutosEnLaNoche,
			int minutosDeHuecos,
			List<String> diasConClase,
			List<String> diasLibres) {
	}

	public record ChoqueDto(String asignatura, String otra) {
	}

	public static PropuestasDeHorarioDto desde(ResultadoDeAcomodo resultado) {
		return new PropuestasDeHorarioDto(
				resultado.propuestas().stream().map(PropuestasDeHorarioDto::propuesta).toList(),
				resultado.choques().stream().map(c -> new ChoqueDto(c.asignatura(), c.otra())).toList(),
				resultado.mensaje(),
				resultado.busquedaIncompleta());
	}

	private static PropuestaDto propuesta(PropuestaDeHorario propuesta) {
		PropuestaDeHorario.Resumen resumen = propuesta.resumen();
		return new PropuestaDto(
				propuesta.puntaje(),
				propuesta.elecciones().stream()
						.map(e -> new EleccionDto(e.asignatura(), e.grupo(), e.bloques().stream().map(PropuestasDeHorarioDto::bloque).toList()))
						.toList(),
				new ResumenDto(resumen.minutosEnLaManana(), resumen.minutosEnLaTarde(), resumen.minutosEnLaNoche(),
						resumen.minutosDeHuecos(), dias(resumen.diasConClase()), dias(resumen.diasLibres())),
				propuesta.motivos());
	}

	private static BloqueDto bloque(BloqueDeOferta bloque) {
		return new BloqueDto(bloque.dia().valorEnBd(), bloque.inicio().toString(), bloque.fin().toString());
	}

	private static List<String> dias(List<DiaDeLaSemana> dias) {
		return dias.stream().map(DiaDeLaSemana::valorEnBd).toList();
	}
}
