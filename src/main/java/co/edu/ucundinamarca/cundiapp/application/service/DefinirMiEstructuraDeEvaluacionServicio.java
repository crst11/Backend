package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.DefinirMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import java.util.List;

/**
 * Guarda cómo evalúan una asignatura (RF05, SCRUM-27).
 *
 * <p>Las dos reglas del 100 % las comprueba el dominio al construir la estructura, así que aquí
 * solo queda lo que el dominio no puede saber: que la matrícula sea de quien está pidiendo.
 */
public class DefinirMiEstructuraDeEvaluacionServicio implements DefinirMiEstructuraDeEvaluacion {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;

	public DefinirMiEstructuraDeEvaluacionServicio(EstructuraDeEvaluacionRepositorio evaluaciones) {
		this.evaluaciones = evaluaciones;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, int idMatricula, List<CategoriaDeEvaluacion> categorias) {
		if (evaluaciones.matricula(idEstudiante, idMatricula).isEmpty()) {
			throw new ReglaDeNegocioVioladaException("No encontramos esa asignatura entre tus matrículas");
		}

		var estructura = new EstructuraDeEvaluacion(idMatricula, categorias);
		evaluaciones.guardar(idEstudiante, estructura);

		return new Resultado(idMatricula, estructura.categorias().size(), estructura.totalDeActividades());
	}
}
