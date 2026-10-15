package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ImportacionDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Asignatura;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaConHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.PeriodoAcademico;
import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Guarda el horario que el estudiante confirmó (RF04, SCRUM-81).
 *
 * <p>Entre el análisis y la confirmación pudo pasar cualquier cosa, así que el período se valida y
 * cada código se vuelve a cruzar contra la ruta de aprendizaje. Lo que no esté en el plan se omite y
 * se cuenta, para poder decírselo en vez de callarlo.
 */
public class ConfirmarImportacionDeHorarioServicio implements ConfirmarImportacionDeHorario {

	private final ImportacionDeHorarioRepositorio horarios;
	private final EstudianteRepositorio estudiantes;
	private final ProgramaAcademicoRepositorio programas;

	public ConfirmarImportacionDeHorarioServicio(
			ImportacionDeHorarioRepositorio horarios,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas) {
		this.horarios = horarios;
		this.estudiantes = estudiantes;
		this.programas = programas;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, Confirmacion confirmacion) {
		PlanDeEstudios plan = estudiantes.planDe(idEstudiante)
				.flatMap(programas::buscarPlanPorCodigo)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Primero elige tu programa: sin la ruta de aprendizaje no sabemos a qué asignaturas corresponde tu horario"));
		PeriodoAcademico periodo = PeriodoAcademico.desdeCodigo(confirmacion.periodo());
		Set<String> delPlan = plan.asignaturas().stream().map(Asignatura::codigo).collect(Collectors.toSet());

		List<AsignaturaConHorario> validas = confirmacion.asignaturas().stream()
				.filter(asignatura -> delPlan.contains(asignatura.codigo()))
				.toList();
		if (validas.isEmpty()) {
			throw new ReglaDeNegocioVioladaException(
					"Ninguna de las asignaturas del reporte está en tu ruta de aprendizaje. Revisa que el reporte sea de tu programa");
		}

		Resultado guardado = horarios.guardar(idEstudiante, periodo, validas);
		int fueraDelPlan = confirmacion.asignaturas().size() - validas.size();
		return new Resultado(
				guardado.nuevas(), guardado.actualizadas(), guardado.omitidas() + fueraDelPlan, guardado.clases());
	}
}
