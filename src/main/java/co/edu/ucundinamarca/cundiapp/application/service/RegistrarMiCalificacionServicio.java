package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.RegistrarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.Nota;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotas;
import java.math.BigDecimal;

/**
 * Anota la nota de una actividad (RF05, SCRUM-28).
 *
 * <p>La escala de la nota la comprueba el dominio ({@link Nota}). Aquí queda lo que el dominio no
 * puede saber: que la asignatura sea del estudiante que pide y que la actividad exista en la
 * estructura que él mismo definió.
 */
public class RegistrarMiCalificacionServicio implements RegistrarMiCalificacion {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;
	private final CalculadoraDeNotas calculadora;

	public RegistrarMiCalificacionServicio(
			EstructuraDeEvaluacionRepositorio evaluaciones, CalculadoraDeNotas calculadora) {
		this.evaluaciones = evaluaciones;
		this.calculadora = calculadora;
	}

	@Override
	public Resultado ejecutar(
			int idEstudiante, int idMatricula, int consecCategoria, int consecActividad, BigDecimal valor) {
		// Primero la nota: si viene mal, no hace falta ir a la base de datos para decirlo.
		Nota nota = new Nota(valor);
		EstructuraDeEvaluacion estructura = estructuraDe(idEstudiante, idMatricula);
		estructura.actividadDe(consecCategoria, consecActividad)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Esa actividad no existe en cómo te evalúan esta asignatura"));

		evaluaciones.guardarNota(idEstudiante, idMatricula, new ClaveDeActividad(consecCategoria, consecActividad), nota);

		var notas = evaluaciones.notasDe(idEstudiante, idMatricula);
		return new Resultado(nota, calculadora.acumular(estructura, notas));
	}

	private EstructuraDeEvaluacion estructuraDe(int idEstudiante, int idMatricula) {
		if (evaluaciones.matricula(idEstudiante, idMatricula).isEmpty()) {
			throw new ReglaDeNegocioVioladaException("No encontramos esa asignatura entre tus matrículas");
		}
		return evaluaciones.buscar(idEstudiante, idMatricula)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Primero define cómo te evalúan en esta asignatura y guárdalo"));
	}
}
