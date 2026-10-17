package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.CrearMiPendiente;
import co.edu.ucundinamarca.cundiapp.application.port.out.EstructuraDeEvaluacionRepositorio;
import co.edu.ucundinamarca.cundiapp.application.port.out.PendienteRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.Pendiente;
import co.edu.ucundinamarca.cundiapp.domain.model.PendienteConSituacion;
import java.time.Clock;
import java.time.Instant;

/**
 * Escribe un pendiente (RF06, SCRUM-29). El dominio comprueba título, fecha y largos; aquí queda lo que
 * no puede saber: que la asignatura, si la hay, sea una de las matrículas del estudiante.
 */
public class CrearMiPendienteServicio implements CrearMiPendiente {

	private final PendienteRepositorio pendientes;
	private final EstructuraDeEvaluacionRepositorio evaluaciones;
	private final Clock reloj;

	public CrearMiPendienteServicio(
			PendienteRepositorio pendientes, EstructuraDeEvaluacionRepositorio evaluaciones, Clock reloj) {
		this.pendientes = pendientes;
		this.evaluaciones = evaluaciones;
		this.reloj = reloj;
	}

	@Override
	public PendienteConSituacion ejecutar(int idEstudiante, Datos datos) {
		Instant ahora = reloj.instant();
		// Primero el pendiente: si viene mal no hace falta ir a la base de datos para decirlo.
		Pendiente nuevo = Pendiente.nuevo(
				idEstudiante, datos.idMatricula(), datos.titulo(), datos.descripcion(), datos.fechaLimite(),
				datos.prioridad(), ahora);
		if (datos.idMatricula() != null && evaluaciones.matricula(idEstudiante, datos.idMatricula()).isEmpty()) {
			throw new ReglaDeNegocioVioladaException("No encontramos esa asignatura entre tus matrículas");
		}
		return PendienteConSituacion.de(pendientes.guardar(nuevo), ahora);
	}
}
