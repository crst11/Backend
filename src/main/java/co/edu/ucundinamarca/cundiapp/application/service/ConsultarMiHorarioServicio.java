package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.FiltroDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.HorarioSemanal;
import co.edu.ucundinamarca.cundiapp.domain.model.OrdenDelHorario;

/**
 * El horario del estudiante (RF04, SCRUM-25 y SCRUM-26). Filtrar y ordenar son reglas del dominio
 * ({@link HorarioSemanal}); aquí solo se arma la respuesta.
 */
public class ConsultarMiHorarioServicio implements ConsultarMiHorario {

	private final HorarioRepositorio horarios;

	public ConsultarMiHorarioServicio(HorarioRepositorio horarios) {
		this.horarios = horarios;
	}

	@Override
	public Resultado ejecutar(int idEstudiante, FiltroDeHorario filtro, OrdenDelHorario orden) {
		var completo = new HorarioSemanal(horarios.clasesDe(idEstudiante));
		return new Resultado(completo.filtrar(filtro), completo.sedes(), completo.asignaturas(), filtro, orden);
	}
}
