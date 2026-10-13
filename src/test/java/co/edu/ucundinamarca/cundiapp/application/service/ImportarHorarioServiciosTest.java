package co.edu.ucundinamarca.cundiapp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario.Confirmacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario.Resultado;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.FuenteDeHorarioPort;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio.MatriculaConClases;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaDeLaSemana;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import co.edu.ucundinamarca.cundiapp.domain.model.ProgramaAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.ReporteDeHorarioDetectado;
import co.edu.ucundinamarca.cundiapp.domain.model.TipoDeAsignatura;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Importar el Reporte de Horario: lo que se cruza con el plan antes de mostrar y de guardar (SCRUM-81). */
@ExtendWith(MockitoExtension.class)
class ImportarHorarioServiciosTest {

	private static final int ESTUDIANTE = 5;
	private static final PeriodoAcademico PERIODO = new PeriodoAcademico(2026, 2);

	@Mock
	private FuenteDeHorarioPort fuente;
	@Mock
	private EstudianteRepositorio estudiantes;
	@Mock
	private ProgramaAcademicoRepositorio programas;
	@Mock
	private ImportacionDeHorarioRepositorio horarios;

	private final PlanDeEstudios plan = new PlanDeEstudios(
			"ISC-2020-FUSA",
			new ProgramaAcademico("109964", "INGENIERÍA DE SISTEMAS Y COMPUTACIÓN", "Ingeniería", "Fusagasugá", 160, 10),
			List.of(
					new Asignatura("CAD1", "Ecuaciones Diferenciales", 3, TipoDeAsignatura.OBLIGATORIA, 4, List.of()),
					new Asignatura("CAD2", "Sistemas Operativos", 3, TipoDeAsignatura.OBLIGATORIA, 5, List.of())));

	private static BloqueDeHorario bloque(DiaDeLaSemana dia) {
		return BloqueDeHorario.delReporte(1, dia, LocalTime.of(9, 0), LocalTime.of(10, 59), null, null);
	}

	private static AsignaturaConHorario clase(String codigo) {
		return new AsignaturaConHorario(codigo, "SIS.FUSA 501M", List.of(bloque(DiaDeLaSemana.MARTES)));
	}

	@BeforeEach
	void elEstudianteTieneSuPlan() {
		lenient().when(estudiantes.planDe(ESTUDIANTE)).thenReturn(Optional.of("ISC-2020-FUSA"));
		lenient().when(programas.buscarPlanPorCodigo("ISC-2020-FUSA")).thenReturn(Optional.of(plan));
	}

	@Nested
	class Analizar {

		private AnalizarReporteDeHorarioServicio servicio() {
			return new AnalizarReporteDeHorarioServicio(fuente, estudiantes, programas, horarios);
		}

		private ReporteDeHorarioDetectado reporte(String programa, AsignaturaConHorario... asignaturas) {
			return new ReporteDeHorarioDetectado(programa, "FUSAGASUGÁ", LocalDate.of(2026, 10, 12), List.of(asignaturas));
		}

		@Test
		void cruzaCadaAsignaturaConLaRutaYTomaElNombreDelPlan() {
			given(fuente.leer(any())).willReturn(reporte("INGENIERIA DE SISTEMAS Y COMPUTACION", clase("CAD1"), clase("XXX")));
			given(horarios.matriculasDe(ESTUDIANTE, PERIODO)).willReturn(List.of());

			var analisis = servicio().ejecutar(ESTUDIANTE, new byte[] {1});

			assertThat(analisis.periodo()).isEqualTo("2026-2");
			assertThat(analisis.coincideConMiPrograma()).isTrue();
			assertThat(analisis.asignaturas()).hasSize(2);
			assertThat(analisis.asignaturas().getFirst().nombre()).isEqualTo("Ecuaciones Diferenciales");
			assertThat(analisis.asignaturas().getFirst().enElPlan()).isTrue();
			assertThat(analisis.asignaturas().get(1).enElPlan()).isFalse();
			assertThat(analisis.asignaturas().get(1).nombre()).isEqualTo("XXX");
			assertThat(analisis.seVanAGuardar()).isEqualTo(1);
			assertThat(analisis.clases()).isEqualTo(2);
		}

		@Test
		void diceQueAsignaturasYaEstabanYCuantasClasesSeReemplazan() {
			given(fuente.leer(any())).willReturn(reporte("INGENIERIA DE SISTEMAS Y COMPUTACION", clase("CAD1"), clase("CAD2")));
			given(horarios.matriculasDe(ESTUDIANTE, PERIODO))
					.willReturn(List.of(new MatriculaConClases("CAD1", "en_curso", 3)));

			var analisis = servicio().ejecutar(ESTUDIANTE, new byte[] {1});

			assertThat(analisis.asignaturas().getFirst().yaEstaba()).isTrue();
			assertThat(analisis.asignaturas().getFirst().clasesQueTiene()).isEqualTo(3);
			assertThat(analisis.asignaturas().get(1).yaEstaba()).isFalse();
		}

		@Test
		void avisaSiElProgramaDelReporteNoEsElQueEligio() {
			given(fuente.leer(any())).willReturn(reporte("DERECHO", clase("CAD1")));
			given(horarios.matriculasDe(ESTUDIANTE, PERIODO)).willReturn(List.of());

			assertThat(servicio().ejecutar(ESTUDIANTE, new byte[] {1}).coincideConMiPrograma()).isFalse();
		}

		@Test
		void sinProgramaElegidoPideElegirloPrimeroYNoLeeNada() {
			given(estudiantes.planDe(ESTUDIANTE)).willReturn(Optional.empty());

			assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, new byte[] {1}))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("elige tu programa");
			verify(fuente, never()).leer(any());
		}
	}

	@Nested
	class Confirmar {

		private ConfirmarImportacionDeHorarioServicio servicio() {
			return new ConfirmarImportacionDeHorarioServicio(horarios, estudiantes, programas);
		}

		@Test
		void guardaSoloLoQueEstaEnElPlanYCuentaLoQueOmite() {
			given(horarios.guardar(ESTUDIANTE, PERIODO, List.of(clase("CAD1"))))
					.willReturn(new Resultado(1, 0, 0, 1));

			var resultado = servicio().ejecutar(
					ESTUDIANTE, new Confirmacion("2026-2", List.of(clase("CAD1"), clase("XXX"))));

			assertThat(resultado).isEqualTo(new Resultado(1, 0, 1, 1));
		}

		@Test
		void siNingunaEstaEnElPlanNoEscribeNada() {
			assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, new Confirmacion("2026-2", List.of(clase("XXX")))))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("ruta de aprendizaje");
			verify(horarios, never()).guardar(anyInt(), any(), any());
		}

		@Test
		void unPeriodoMalEscritoSeRechazaAntesDeGuardar() {
			assertThatThrownBy(() -> servicio().ejecutar(ESTUDIANTE, new Confirmacion("2026/2", List.of(clase("CAD1")))))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("año-semestre");
			verify(horarios, never()).guardar(anyInt(), any(), any());
		}

		@Test
		void unaConfirmacionVaciaSeRechaza() {
			assertThatThrownBy(() -> new Confirmacion("2026-2", List.of()))
					.isInstanceOf(ReglaDeNegocioVioladaException.class)
					.hasMessageContaining("nada que confirmar");
		}
	}
}
