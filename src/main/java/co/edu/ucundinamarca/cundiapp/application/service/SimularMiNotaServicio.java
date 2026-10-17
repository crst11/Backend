package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.SimularMiNota;
import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotaRequerida;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La nota que hace falta, con la nota real del estudiante o con notas hipotéticas encima (RF08,
 * SCRUM-33 y SCRUM-34).
 *
 * <p>Solo lee: ni la simulación ni la meta de la petición se guardan. Una asignatura sin estructura
 * de evaluación no se proyecta, porque una cuenta sobre pesos que nadie ha dicho no significa nada.
 */
public class SimularMiNotaServicio implements SimularMiNota {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;
	private final ConfiguracionRepositorio configuracion;
	private final CalculadoraDeNotaRequerida calculadora;

	public SimularMiNotaServicio(
			EstructuraDeEvaluacionRepositorio evaluaciones,
			ConfiguracionRepositorio configuracion,
			CalculadoraDeNotaRequerida calculadora) {
		this.evaluaciones = evaluaciones;
		this.configuracion = configuracion;
		this.calculadora = calculadora;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, int idMatricula, Meta meta, List<NotaSimulada> simuladas) {
		AsignaturaMatriculada asignatura = evaluaciones.matricula(idEstudiante, idMatricula)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"No encontramos esa asignatura entre tus matrículas"));
		EstructuraDeEvaluacion estructura = evaluaciones.buscar(idEstudiante, idMatricula)
				.filter(EstructuraDeEvaluacion::estaDefinida)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Define primero cómo te evalúan en esta asignatura para poder calcular la nota que necesitas"));

		Map<ClaveDeActividad, Nota> hipoteticas = hipoteticas(estructura, simuladas);
		NotasDeLaAsignatura notas = evaluaciones.notasDe(idEstudiante, idMatricula).conSimuladas(hipoteticas);
		Meta aCalcular = meta != null ? meta : configuracion.metaDe(idEstudiante);

		return new Resultado(asignatura, calculadora.proyectar(estructura, notas, aCalcular));
	}

	/** Cada nota tiene que caer en una actividad que existe y no puede repetirse la misma actividad. */
	private static Map<ClaveDeActividad, Nota> hipoteticas(EstructuraDeEvaluacion estructura, List<NotaSimulada> simuladas) {
		Map<ClaveDeActividad, Nota> resultado = new HashMap<>();
		for (NotaSimulada simulada : simuladas) {
			ClaveDeActividad clave = simulada.actividad();
			if (estructura.actividadDe(clave.consecCategoria(), clave.consecActividad()).isEmpty()) {
				throw new ReglaDeNegocioVioladaException("Una de las actividades no está en la estructura de esa asignatura");
			}
			if (resultado.put(clave, simulada.nota()) != null) {
				throw new ReglaDeNegocioVioladaException("Mandaste dos notas para la misma actividad");
			}
		}
		return resultado;
	}
}
