package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotas;
import java.util.List;
import java.util.Map;

/**
 * Cómo se evalúa una asignatura y cómo va en ella (RF05, SCRUM-27 y SCRUM-28).
 *
 * <p>Una asignatura sin configurar no devuelve una pantalla vacía: devuelve la plantilla de la
 * universidad como propuesta, que es el criterio "viene con la plantilla 30/30/40 que puedo
 * ajustar". La propuesta no se guarda sola, porque guardar algo que el estudiante no ha mirado
 * sería afirmar que así lo evalúan. Una propuesta no tiene notas: no hay dónde ponerlas todavía.
 */
public class ConsultarMiEstructuraDeEvaluacionServicio implements ConsultarMiEstructuraDeEvaluacion {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;
	private final CalculadoraDeNotas calculadora;

	public ConsultarMiEstructuraDeEvaluacionServicio(
			EstructuraDeEvaluacionRepositorio evaluaciones, CalculadoraDeNotas calculadora) {
		this.evaluaciones = evaluaciones;
		this.calculadora = calculadora;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, int idMatricula) {
		AsignaturaMatriculada asignatura = evaluaciones.matricula(idEstudiante, idMatricula)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"No encontramos esa asignatura entre tus matrículas"));

		return evaluaciones.buscar(idEstudiante, idMatricula)
				.map(estructura -> guardada(asignatura, estructura, idEstudiante))
				.orElseGet(() -> propuesta(asignatura, idMatricula));
	}

	private Resultado guardada(AsignaturaMatriculada asignatura, EstructuraDeEvaluacion estructura, int idEstudiante) {
		NotasDeLaAsignatura notas = evaluaciones.notasDe(idEstudiante, estructura.idMatricula());
		return new Resultado(asignatura, estructura, true, notas, calculadora.acumular(estructura, notas));
	}

	/** Si no hay plantilla predeterminada en la base, se propone una estructura vacía. */
	private Resultado propuesta(AsignaturaMatriculada asignatura, int idMatricula) {
		EstructuraDeEvaluacion estructura = evaluaciones.plantillaPredeterminada()
				.map(plantilla -> EstructuraDeEvaluacion.desdePlantilla(idMatricula, plantilla))
				.orElseGet(() -> new EstructuraDeEvaluacion(idMatricula, List.of()));
		NotasDeLaAsignatura sinNotas = new NotasDeLaAsignatura(idMatricula, Map.of());
		return new Resultado(asignatura, estructura, false, sinNotas, calculadora.acumular(estructura, sinNotas));
	}
}
