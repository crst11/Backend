package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHistorial;
import co.edu.ucundinamarca.cundiapp.application.port.in.MiHistorial;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstudianteRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.HistorialAcademicoRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.ProgramaAcademicoRepositorio;

/**
 * Reúne lo que hace falta para leer el historial: lo cursado, los promedios oficiales que trajo el
 * reporte y el programa contra el que se mide el avance (SCRUM-22).
 *
 * <p>Sin programa elegido todavía hay historial: lo único que no se puede decir es qué porcentaje
 * de la carrera lleva, porque no hay un total contra el cual compararlo.
 */
public class ConsultarMiHistorialServicio implements ConsultarMiHistorial {

	private final HistorialAcademicoRepositorio historiales;
	private final EstudianteRepositorio estudiantes;
	private final ProgramaAcademicoRepositorio programas;

	public ConsultarMiHistorialServicio(
			HistorialAcademicoRepositorio historiales,
			EstudianteRepositorio estudiantes,
			ProgramaAcademicoRepositorio programas) {
		this.historiales = historiales;
		this.estudiantes = estudiantes;
		this.programas = programas;
	}

	@Override
	public MiHistorial ejecutar(int idEstudiante) {
		return new MiHistorial(
				historiales.historialDe(idEstudiante),
				historiales.resumenesOficialesDe(idEstudiante),
				estudiantes.planDe(idEstudiante)
						.flatMap(programas::buscarPlanPorCodigo)
						.map(plan -> plan.programa())
						.orElse(null));
	}
}
