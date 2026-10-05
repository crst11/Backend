package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import java.util.List;

/**
 * Cómo se evalúa una asignatura (RF05, SCRUM-27).
 *
 * <p>Una asignatura sin configurar no devuelve una pantalla vacía: devuelve la plantilla de la
 * universidad como propuesta, que es el criterio "viene con la plantilla 30/30/40 que puedo
 * ajustar". La propuesta no se guarda sola, porque guardar algo que el estudiante no ha mirado
 * sería afirmar que así lo evalúan.
 */
public class ConsultarMiEstructuraDeEvaluacionServicio implements ConsultarMiEstructuraDeEvaluacion {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;

	public ConsultarMiEstructuraDeEvaluacionServicio(EstructuraDeEvaluacionRepositorio evaluaciones) {
		this.evaluaciones = evaluaciones;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, int idMatricula) {
		AsignaturaMatriculada asignatura = evaluaciones.matricula(idEstudiante, idMatricula)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"No encontramos esa asignatura entre tus matrículas"));

		return evaluaciones.buscar(idEstudiante, idMatricula)
				.map(estructura -> new Resultado(asignatura, estructura, true))
				.orElseGet(() -> new Resultado(asignatura, propuesta(idMatricula), false));
	}

	/** Si no hay plantilla predeterminada en la base, se propone una estructura vacía. */
	private EstructuraDeEvaluacion propuesta(int idMatricula) {
		return evaluaciones.plantillaPredeterminada()
				.map(plantilla -> EstructuraDeEvaluacion.desdePlantilla(idMatricula, plantilla))
				.orElseGet(() -> new EstructuraDeEvaluacion(idMatricula, List.of()));
	}
}
