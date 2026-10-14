package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Cuánta de su información lleva cargada el estudiante y qué le falta (RF03, SCRUM-84). */
class AvanceDeInformacionTest {

	private static AsignaturaMatriculada enCurso(int id, String nombre, int actividades) {
		return new AsignaturaMatriculada(
				id, "CAD" + id, nombre, 3, "2026-2", EstadoDeAsignatura.EN_CURSO, actividades > 0 ? 3 : 0, actividades);
	}

	@Test
	void sinNadaCargadoLosDosReportesFaltanYElAvanceEsCero() {
		var avance = AvanceDeInformacion.de(false, false, List.of());

		assertThat(avance.total()).isEqualTo(2);
		assertThat(avance.hechos()).isZero();
		assertThat(avance.porcentaje()).isZero();
		assertThat(avance.pasos()).extracting(PasoDeAvance::tipo)
				.containsExactly(TipoDePaso.REGISTRO_EXTENDIDO, TipoDePaso.HORARIO);
	}

	@Test
	void porCadaAsignaturaEnCursoHayUnPasoMasQueSeCumpleConSusActividades() {
		var avance = AvanceDeInformacion.de(true, true, List.of(enCurso(1, "Álgebra", 8), enCurso(2, "Física", 0)));

		assertThat(avance.total()).isEqualTo(4);
		assertThat(avance.hechos()).isEqualTo(3);
		assertThat(avance.pasos().get(2).hecho()).isTrue();
		assertThat(avance.pasos().get(3).hecho()).isFalse();
		assertThat(avance.pasos().get(3).idMatricula()).isEqualTo(2);
	}

	@Test
	void conCincoMateriasHaySieteYConDosListasMasLosReportesVan57PorCiento() {
		var materias = List.of(
				enCurso(1, "A", 4), enCurso(2, "B", 4), enCurso(3, "C", 0), enCurso(4, "D", 0), enCurso(5, "E", 0));

		// Dos reportes y dos materias: 4 de 7 = 57 %.
		var avance = AvanceDeInformacion.de(true, true, materias);

		assertThat(avance.total()).isEqualTo(7);
		assertThat(avance.hechos()).isEqualTo(4);
		assertThat(avance.porcentaje()).isEqualTo(57);
	}

	@Test
	void elPorcentajeNoLlegaACienHastaQueNoFalteNada() {
		// 2 de 3 son 66,6 %: se muestra 66, no 67, para no decir «casi listo» de más.
		assertThat(AvanceDeInformacion.de(true, true, List.of(enCurso(1, "A", 0))).porcentaje()).isEqualTo(66);
		assertThat(AvanceDeInformacion.de(true, true, List.of(enCurso(1, "A", 2))).porcentaje()).isEqualTo(100);
	}

	@Test
	void unaAsignaturaQueYaTerminoNoCuentaPorqueNoEstaEnCurso() {
		var aprobada = new AsignaturaMatriculada(9, "CAD9", "Vieja", 3, "2025-1", EstadoDeAsignatura.APROBADA, 0, 0);

		var avance = AvanceDeInformacion.de(true, true, List.of(aprobada));

		assertThat(avance.total()).isEqualTo(2);
	}

	@Test
	void cadaPasoDiceQueSeLePideAlEstudiante() {
		var avance = AvanceDeInformacion.de(false, true, List.of(enCurso(1, "Álgebra Lineal", 0)));

		assertThat(avance.pasos()).extracting(PasoDeAvance::texto).containsExactly(
				"Registro Académico Extendido", "Reporte de Horario", "Actividades de Álgebra Lineal");
	}
}
