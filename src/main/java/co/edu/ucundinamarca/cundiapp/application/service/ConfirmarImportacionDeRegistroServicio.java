package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Guarda lo que el estudiante confirmó (RF03, SCRUM-23).
 *
 * <p>Lo que llega no se da por bueno: el estudiante pudo corregir una nota antes de confirmar, y
 * entre el análisis y la confirmación pudo pasar cualquier cosa. Cada código se vuelve a cruzar
 * contra la ruta de aprendizaje y cada nota se valida contra la escala de 0.0 a 5.0. Lo que no
 * esté en el plan se omite y se cuenta, para poder decírselo en vez de callarlo.
 */
public class ConfirmarImportacionDeRegistroServicio implements ConfirmarImportacionDeRegistro {

	private static final BigDecimal NOTA_MINIMA = BigDecimal.ZERO;
	private static final BigDecimal NOTA_MAXIMA = new BigDecimal("5.0");

	private final ImportacionRepositorio importaciones;
	private final EstudianteRepositorio estudiantes;
	private final ProgramaAcademicoRepositorio programas;

	public ConfirmarImportacionDeRegistroServicio(
			ImportacionRepositorio importaciones,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas) {
		this.importaciones = importaciones;
		this.estudiantes = estudiantes;
		this.programas = programas;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, Confirmacion confirmacion) {
		PlanDeEstudios plan = estudiantes.planDe(idEstudiante)
				.flatMap(programas::buscarPlanPorCodigo)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Primero elige tu programa: sin la ruta de aprendizaje no sabemos a qué asignaturas corresponden tus notas"));
		Set<String> delPlan = plan.asignaturas().stream()
				.map(Asignatura::codigo)
				.collect(Collectors.toSet());

		List<PeriodoConfirmado> validos = confirmacion.periodos().stream()
				.map(periodo -> validar(periodo, delPlan))
				.filter(periodo -> !periodo.notas().isEmpty())
				.toList();
		if (validos.isEmpty()) {
			throw new ReglaDeNegocioVioladaException(
					"Ninguna de las asignaturas del reporte está en tu ruta de aprendizaje. Revisa que el reporte sea de tu programa");
		}

		return importaciones.guardar(
				idEstudiante, confirmacion.nombreArchivo(), confirmacion.detectadas(), validos);
	}

	private static PeriodoConfirmado validar(PeriodoConfirmado periodo, Set<String> delPlan) {
		// Valida el código del período aunque aquí no se use: si viene mal, falla ahora y no al guardar.
		PeriodoAcademico.desdeCodigo(periodo.codigo());

		List<NotaConfirmada> notas = periodo.notas().stream()
				.filter(nota -> delPlan.contains(nota.codigoAsignatura()))
				.peek(ConfirmarImportacionDeRegistroServicio::validarNota)
				.toList();
		return new PeriodoConfirmado(
				periodo.codigo(),
				periodo.creditosMatriculados(),
				periodo.creditosAprobados(),
				periodo.promedioPeriodo(),
				periodo.promedioAcumulado(),
				notas);
	}

	private static void validarNota(NotaConfirmada nota) {
		if (nota.nota() == null) {
			return;
		}
		if (nota.nota().compareTo(NOTA_MINIMA) < 0 || nota.nota().compareTo(NOTA_MAXIMA) > 0) {
			throw new ReglaDeNegocioVioladaException(
					"La nota de " + nota.codigoAsignatura() + " debe estar entre 0.0 y 5.0");
		}
	}
}
