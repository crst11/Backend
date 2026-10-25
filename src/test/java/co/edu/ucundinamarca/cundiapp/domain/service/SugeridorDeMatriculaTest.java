package co.edu.ucundinamarca.cundiapp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaCursada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstadoDeAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.HistorialAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.MateriaSugerida;
import co.edu.ucundinamarca.cundiapp.domain.model.MotivoDeSugerencia;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoCursado;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.SugerenciaDeMatricula;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAsignatura;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** El motor que propone qué matricular el próximo semestre a partir del plan y del historial (RF13, SCRUM-244). */
class SugeridorDeMatriculaTest {

	private static final ProgramaAcademico PROGRAMA = new ProgramaAcademico("P1", "Ingeniería", "Facultad", "Sede", 100, 10);

	private final SugeridorDeMatricula sugeridor = new SugeridorDeMatricula();

	private static Asignatura asignatura(String codigo, int creditos, int periodo, String... prerrequisitos) {
		return new Asignatura(codigo, "Materia " + codigo, creditos, TipoDeAsignatura.OBLIGATORIA, periodo,
				List.of(prerrequisitos));
	}

	private static PlanDeEstudios plan(Asignatura... asignaturas) {
		return new PlanDeEstudios("PLAN", PROGRAMA, List.of(asignaturas));
	}

	private static AsignaturaCursada cursada(String codigo, int creditos, EstadoDeAsignatura estado) {
		BigDecimal nota = estado == EstadoDeAsignatura.EN_CURSO ? null : new BigDecimal("3.5");
		return new AsignaturaCursada(codigo, "Materia " + codigo, creditos, nota, estado);
	}

	private static HistorialAcademico historial(AsignaturaCursada... cursadas) {
		return new HistorialAcademico(List.of(new PeriodoCursado("2026-1", List.of(cursadas))));
	}

	private static List<String> codigos(List<MateriaSugerida> materias) {
		return materias.stream().map(MateriaSugerida::codigo).toList();
	}

	private static MateriaSugerida de(SugerenciaDeMatricula sugerencia, String codigo) {
		return sugerencia.materias().stream().filter(m -> m.codigo().equals(codigo)).findFirst().orElseThrow();
	}

	@Test
	void sinNingunaMateriaCursadaSugiereElPrimerSemestre() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 4, 1), asignatura("C", 3, 2, "A"));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, new HistorialAcademico(List.of()));

		assertThat(sugerencia.semestreActual()).isNull();
		assertThat(sugerencia.semestreSugerido()).isEqualTo(1);
		assertThat(codigos(sugerencia.materias())).containsExactly("A", "B");
		assertThat(sugerencia.creditos()).isEqualTo(7);
	}

	@Test
	void conMateriasEnCursoSugiereElSemestreSiguienteAlQueMasCursa() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2, "A"), asignatura("C", 4, 3, "B"));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.APROBADA),
				cursada("B", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(sugerencia.semestreActual()).isEqualTo(2);
		assertThat(sugerencia.semestreSugerido()).isEqualTo(3);
		assertThat(codigos(sugerencia.materias())).containsExactly("C");
	}

	@Test
	void elSemestreActualEsElQueTieneMasCreditosEnCursoYEnEmpateElMenor() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 4, 2), asignatura("C", 4, 3),
				asignatura("D", 3, 4));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.EN_CURSO),
				cursada("B", 4, EstadoDeAsignatura.EN_CURSO), cursada("C", 4, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(sugerencia.semestreActual()).isEqualTo(2);
		assertThat(sugerencia.semestreSugerido()).isEqualTo(3);
	}

	@Test
	void unaMateriaAtrasadaSeMarcaComoPendiente() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2), asignatura("C", 3, 3));
		HistorialAcademico historial = historial(cursada("B", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(codigos(sugerencia.materias())).containsExactly("A", "C");
		assertThat(de(sugerencia, "A").motivo()).isEqualTo(MotivoDeSugerencia.PENDIENTE);
		assertThat(de(sugerencia, "C").motivo()).isEqualTo(MotivoDeSugerencia.SIGUIENTE_SEMESTRE);
	}

	@Test
	void unaMateriaReprobadaSeMarcaParaRepetir() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2), asignatura("C", 3, 3));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.REPROBADA),
				cursada("B", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(de(sugerencia, "A").motivo()).isEqualTo(MotivoDeSugerencia.A_REPETIR);
	}

	@Test
	void unaReprobadaQueDespuesSeAproboNoSeSugiere() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2));
		HistorialAcademico historial = new HistorialAcademico(List.of(
				new PeriodoCursado("2025-2", List.of(cursada("A", 3, EstadoDeAsignatura.REPROBADA))),
				new PeriodoCursado("2026-1", List.of(cursada("A", 3, EstadoDeAsignatura.APROBADA),
						cursada("B", 3, EstadoDeAsignatura.EN_CURSO)))));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(sugerencia.materias()).isEmpty();
	}

	@Test
	void unaMateriaCanceladaNoCuentaComoCursadaYSeSugiereDeNuevo() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.CANCELADA),
				cursada("B", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(codigos(sugerencia.materias())).containsExactly("A");
		assertThat(de(sugerencia, "A").motivo()).isEqualTo(MotivoDeSugerencia.PENDIENTE);
	}

	@Test
	void sinElPrerrequisitoAprobadoLaMateriaNoSeSugiere() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2, "A"), asignatura("C", 3, 3, "A"));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.REPROBADA),
				cursada("B", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(codigos(sugerencia.materias())).containsExactly("A");
	}

	@Test
	void unPrerrequisitoEnCursoCuentaComoCumplidoYAvisaQueDepende() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2, "A"));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(codigos(sugerencia.materias())).containsExactly("B");
		assertThat(de(sugerencia, "B").dependeDeUnaEnCurso()).isTrue();
	}

	@Test
	void unPrerrequisitoAprobadoNoAvisaDependencia() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2, "A"));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.APROBADA));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(de(sugerencia, "B").dependeDeUnaEnCurso()).isFalse();
	}

	@Test
	void lasMateriasDeSemestresPosterioresConPrerrequisitosCumplidosSonAdelantables() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2), asignatura("C", 3, 3),
				asignatura("D", 3, 4, "C"), asignatura("E", 2, 5, "A"));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.APROBADA),
				cursada("B", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(codigos(sugerencia.materias())).containsExactly("C");
		assertThat(codigos(sugerencia.adelantables())).containsExactly("E");
		assertThat(de(sugerencia, "C").motivo()).isEqualTo(MotivoDeSugerencia.SIGUIENTE_SEMESTRE);
		assertThat(sugerencia.adelantables().getFirst().motivo()).isEqualTo(MotivoDeSugerencia.ADELANTABLE);
	}

	@Test
	void lasMateriasSeOrdenanPorSemestreYCodigo() {
		PlanDeEstudios plan = plan(asignatura("Z", 3, 1), asignatura("B", 3, 1), asignatura("M", 3, 2));
		HistorialAcademico historial = historial(cursada("M", 3, EstadoDeAsignatura.CANCELADA));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(codigos(sugerencia.materias())).containsExactly("B", "Z");
		assertThat(codigos(sugerencia.adelantables())).containsExactly("M");
	}

	@Test
	void cuandoNoQuedaNadaPorMatricularNoHaySemestreSugerido() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.APROBADA),
				cursada("B", 3, EstadoDeAsignatura.EN_CURSO));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(sugerencia.semestreSugerido()).isNull();
		assertThat(sugerencia.materias()).isEmpty();
		assertThat(sugerencia.creditos()).isZero();
	}

	@Test
	void lasMateriasSinSemestreEnElPlanNoSeSugieren() {
		Asignatura sinSemestre = new Asignatura("X", "Libre", 2, TipoDeAsignatura.ELECTIVA, null, List.of());
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), sinSemestre);

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, new HistorialAcademico(List.of()));

		assertThat(codigos(sugerencia.materias())).containsExactly("A");
	}

	@Test
	void sinMateriasEnCursoElSemestreSugeridoEsElPrimeroConMateriasSinAprobar() {
		PlanDeEstudios plan = plan(asignatura("A", 3, 1), asignatura("B", 3, 2), asignatura("C", 3, 3));
		HistorialAcademico historial = historial(cursada("A", 3, EstadoDeAsignatura.APROBADA));

		SugerenciaDeMatricula sugerencia = sugeridor.sugerir(plan, historial);

		assertThat(sugerencia.semestreActual()).isNull();
		assertThat(sugerencia.semestreSugerido()).isEqualTo(2);
		assertThat(codigos(sugerencia.materias())).containsExactly("B");
		assertThat(codigos(sugerencia.adelantables())).containsExactly("C");
	}
}
