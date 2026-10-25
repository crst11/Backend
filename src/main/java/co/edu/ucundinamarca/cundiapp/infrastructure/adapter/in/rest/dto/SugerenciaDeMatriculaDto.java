package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.SugerenciaParaMatricular;
import co.edu.ucundinamarca.cundiapp.domain.model.MateriaSugerida;
import co.edu.ucundinamarca.cundiapp.domain.model.SugerenciaDeMatricula;
import java.util.List;
import java.util.Locale;

/**
 * Qué matricular el próximo semestre (RF13, SCRUM-244). Sin sugerencia, {@code semestreSugerido} es nulo y
 * {@code motivo} dice por qué, para que la pantalla caiga al armado manual.
 *
 * @param adelantables materias de semestres posteriores con sus prerrequisitos ya aprobados
 * @param creditosDelSemestre lo que el plan asigna al semestre sugerido: el valor inicial de «créditos que puedes llevar»
 */
public record SugerenciaDeMatriculaDto(
		Integer semestreActual,
		Integer semestreSugerido,
		List<MateriaDto> materias,
		int creditos,
		List<MateriaDto> adelantables,
		int creditosDelSemestre,
		String motivo) {

	/**
	 * @param motivoDeSugerencia {@code siguiente_semestre}, {@code pendiente}, {@code a_repetir} o {@code adelantable}
	 * @param prerrequisitos nombres de las materias que pide el plan; vacío si no pide ninguna
	 */
	public record MateriaDto(
			String codigo,
			String nombre,
			int creditos,
			int semestre,
			String motivoDeSugerencia,
			boolean dependeDeUnaEnCurso,
			List<String> prerrequisitos) {
	}

	public static SugerenciaDeMatriculaDto desde(SugerenciaParaMatricular resultado) {
		SugerenciaDeMatricula sugerencia = resultado.sugerencia();
		String motivo = resultado.razon() == null ? null : textoDe(resultado.razon());
		if (sugerencia == null) {
			return new SugerenciaDeMatriculaDto(null, null, List.of(), 0, List.of(), 0, motivo);
		}
		return new SugerenciaDeMatriculaDto(
				sugerencia.semestreActual(),
				sugerencia.semestreSugerido(),
				sugerencia.materias().stream().map(SugerenciaDeMatriculaDto::materia).toList(),
				sugerencia.creditos(),
				sugerencia.adelantables().stream().map(SugerenciaDeMatriculaDto::materia).toList(),
				sugerencia.creditosDelSemestre(),
				motivo);
	}

	private static MateriaDto materia(MateriaSugerida materia) {
		return new MateriaDto(materia.codigo(), materia.nombre(), materia.creditos(), materia.periodoSugerido(),
				materia.motivo().name().toLowerCase(Locale.ROOT), materia.dependeDeUnaEnCurso(), materia.prerrequisitos());
	}

	private static String textoDe(SugerenciaParaMatricular.Razon razon) {
		return switch (razon) {
			case SIN_PLAN -> "Elige tu programa para recibir una sugerencia de matrícula.";
			case SIN_HISTORIAL -> "Importa tu Registro Académico Extendido para recibir una sugerencia de matrícula.";
			case NADA_POR_MATRICULAR -> "Ya aprobaste o cursas todas las asignaturas de tu plan.";
		};
	}
}
