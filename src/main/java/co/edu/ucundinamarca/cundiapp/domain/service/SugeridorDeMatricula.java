package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaCursada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.HistorialAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.MateriaSugerida;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeSugerencia;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.SugerenciaDeMatricula;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Propone qué matricular el próximo semestre a partir del plan de estudios y de lo que el estudiante lleva (RF13,
 * SCRUM-244).
 *
 * <p>El semestre actual es aquel donde el estudiante cursa más créditos ahora (en empate, el menor); el sugerido es el
 * siguiente. Sin nada en curso, el sugerido es el primer semestre con asignaturas sin aprobar. Un prerrequisito que se
 * cursa ahora cuenta como cumplido, y la materia lo avisa. Es Java puro: no consulta nada ni guarda nada.
 */
public class SugeridorDeMatricula {

	private static final Comparator<MateriaSugerida> POR_SEMESTRE_Y_CODIGO =
			Comparator.comparingInt(MateriaSugerida::periodoSugerido).thenComparing(MateriaSugerida::codigo);

	public SugerenciaDeMatricula sugerir(PlanDeEstudios plan, HistorialAcademico historial) {
		List<AsignaturaCursada> cursadas = historial.periodos().stream()
				.flatMap(periodo -> periodo.asignaturas().stream())
				.toList();
		Set<String> aprobadas = codigosEn(cursadas, EstadoDeAsignatura.APROBADA);
		Set<String> enCurso = codigosEn(cursadas, EstadoDeAsignatura.EN_CURSO);
		Set<String> reprobadas = codigosEn(cursadas, EstadoDeAsignatura.REPROBADA);

		List<Asignatura> candidatas = plan.asignaturas().stream()
				.filter(asignatura -> asignatura.periodoSugerido() != null)
				.filter(asignatura -> !aprobadas.contains(asignatura.codigo()) && !enCurso.contains(asignatura.codigo()))
				.toList();

		Integer semestreActual = semestreActual(plan, enCurso);
		Integer semestreSugerido = semestreSugerido(semestreActual, candidatas);
		if (semestreSugerido == null) {
			return new SugerenciaDeMatricula(semestreActual, null, List.of(), List.of());
		}

		List<MateriaSugerida> materias = candidatas.stream()
				.filter(asignatura -> asignatura.periodoSugerido() <= semestreSugerido)
				.filter(asignatura -> cumplePrerrequisitos(asignatura, aprobadas, enCurso))
				.map(asignatura -> sugerida(asignatura, motivoDe(asignatura, semestreSugerido, reprobadas), enCurso))
				.sorted(POR_SEMESTRE_Y_CODIGO)
				.toList();
		List<MateriaSugerida> adelantables = candidatas.stream()
				.filter(asignatura -> asignatura.periodoSugerido() > semestreSugerido)
				.filter(asignatura -> cumplePrerrequisitos(asignatura, aprobadas, enCurso))
				.map(asignatura -> sugerida(asignatura, MotivoDeSugerencia.ADELANTABLE, enCurso))
				.sorted(POR_SEMESTRE_Y_CODIGO)
				.toList();
		return new SugerenciaDeMatricula(semestreActual, semestreSugerido, materias, adelantables);
	}

	private static Set<String> codigosEn(List<AsignaturaCursada> cursadas, EstadoDeAsignatura estado) {
		return cursadas.stream()
				.filter(cursada -> cursada.estado() == estado)
				.map(AsignaturaCursada::codigo)
				.collect(Collectors.toSet());
	}

	/** El semestre del plan con más créditos en curso; en empate, el menor. Nulo si no cursa nada del plan. */
	private static Integer semestreActual(PlanDeEstudios plan, Set<String> enCurso) {
		Map<Integer, Integer> creditosPorSemestre = new HashMap<>();
		for (Asignatura asignatura : plan.asignaturas()) {
			if (asignatura.periodoSugerido() != null && enCurso.contains(asignatura.codigo())) {
				creditosPorSemestre.merge(asignatura.periodoSugerido(), asignatura.creditos(), Integer::sum);
			}
		}
		return creditosPorSemestre.entrySet().stream()
				.sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	private static Integer semestreSugerido(Integer semestreActual, List<Asignatura> candidatas) {
		if (candidatas.isEmpty()) {
			return null;
		}
		if (semestreActual != null) {
			return semestreActual + 1;
		}
		return candidatas.stream().mapToInt(Asignatura::periodoSugerido).min().orElseThrow();
	}

	private static boolean cumplePrerrequisitos(Asignatura asignatura, Set<String> aprobadas, Set<String> enCurso) {
		return asignatura.prerrequisitos().stream()
				.allMatch(codigo -> aprobadas.contains(codigo) || enCurso.contains(codigo));
	}

	private static MotivoDeSugerencia motivoDe(Asignatura asignatura, int semestreSugerido, Set<String> reprobadas) {
		if (reprobadas.contains(asignatura.codigo())) {
			return MotivoDeSugerencia.A_REPETIR;
		}
		return asignatura.periodoSugerido() < semestreSugerido
				? MotivoDeSugerencia.PENDIENTE
				: MotivoDeSugerencia.SIGUIENTE_SEMESTRE;
	}

	private static MateriaSugerida sugerida(Asignatura asignatura, MotivoDeSugerencia motivo, Set<String> enCurso) {
		boolean dependeDeUnaEnCurso = asignatura.prerrequisitos().stream().anyMatch(enCurso::contains);
		return new MateriaSugerida(asignatura.codigo(), asignatura.nombre(), asignatura.creditos(),
				asignatura.periodoSugerido(), motivo, dependeDeUnaEnCurso);
	}
}
