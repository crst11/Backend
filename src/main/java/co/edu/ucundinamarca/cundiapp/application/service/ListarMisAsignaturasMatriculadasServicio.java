package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ListarMisAsignaturasMatriculadas;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import java.util.List;

/** Las asignaturas matriculadas del estudiante (SCRUM-27). */
public class ListarMisAsignaturasMatriculadasServicio implements ListarMisAsignaturasMatriculadas {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;

	public ListarMisAsignaturasMatriculadasServicio(EstructuraDeEvaluacionRepositorio evaluaciones) {
		this.evaluaciones = evaluaciones;
	}

	@Override
	public List<AsignaturaMatriculada> ejecutar(int idEstudiante) {
		return evaluaciones.matriculasDe(idEstudiante);
	}
}
