package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.QuitarMiCalificacion;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaveDeActividad;
import co.edu.ucundinamarca.cundiapp.domain.model.EstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;
import co.edu.ucundinamarca.cundiapp.domain.service.CalculadoraDeNotas;

/**
 * Quita la nota de una actividad (RF05, SCRUM-28). Quitar una nota que no existe se rechaza, igual
 * que cerrar una sesión que ya estaba cerrada: es señal de que la pantalla quedó desactualizada.
 */
public class QuitarMiCalificacionServicio implements QuitarMiCalificacion {

	private final EstructuraDeEvaluacionRepositorio evaluaciones;
	private final CalculadoraDeNotas calculadora;

	public QuitarMiCalificacionServicio(
			EstructuraDeEvaluacionRepositorio evaluaciones, CalculadoraDeNotas calculadora) {
		this.evaluaciones = evaluaciones;
		this.calculadora = calculadora;
	}

	@Override
	public NotaAcumulada ejecutar(int idEstudiante, int idMatricula, int consecCategoria, int consecActividad) {
		if (evaluaciones.matricula(idEstudiante, idMatricula).isEmpty()) {
			throw new ReglaDeNegocioVioladaException("No encontramos esa asignatura entre tus matrículas");
		}
		EstructuraDeEvaluacion estructura = evaluaciones.buscar(idEstudiante, idMatricula)
				.orElseThrow(() -> new ReglaDeNegocioVioladaException(
						"Primero define cómo te evalúan en esta asignatura y guárdalo"));

		boolean existia = evaluaciones.quitarNota(
				idEstudiante, idMatricula, new ClaveDeActividad(consecCategoria, consecActividad));
		if (!existia) {
			throw new ReglaDeNegocioVioladaException("Esa actividad no tiene una nota registrada");
		}
		return calculadora.acumular(estructura, evaluaciones.notasDe(idEstudiante, idMatricula));
	}
}
