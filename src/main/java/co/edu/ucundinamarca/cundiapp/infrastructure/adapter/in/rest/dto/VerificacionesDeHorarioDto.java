package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.ChoqueDelGrupo;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDelCandidato;
import java.util.List;

/**
 * Por cada grupo que se probó, si cabe en el horario o con qué choca (RF13, SCRUM-245): el texto lo arma la
 * pantalla con lo que dice cada choque (materia, grupo, día sin tildes y horas como {@code 08:00}).
 *
 * @param candidatos en el mismo orden en que llegaron
 */
public record VerificacionesDeHorarioDto(List<CandidatoDto> candidatos) {

	public record CandidatoDto(String grupo, boolean cabe, List<ChoqueDto> choques) {
	}

	/** La clase del horario armado con la que choca el grupo probado. */
	public record ChoqueDto(String asignatura, String grupo, String dia, String inicio, String fin) {
	}

	public static VerificacionesDeHorarioDto desde(List<ResultadoDelCandidato> resultados) {
		return new VerificacionesDeHorarioDto(resultados.stream()
				.map(r -> new CandidatoDto(r.grupo(), r.cabe(), r.choques().stream().map(VerificacionesDeHorarioDto::choque).toList()))
				.toList());
	}

	private static ChoqueDto choque(ChoqueDelGrupo choque) {
		return new ChoqueDto(choque.asignatura(), choque.grupo(), choque.dia().valorEnBd(),
				choque.inicio().toString(), choque.fin().toString());
	}
}
