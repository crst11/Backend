package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.HorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.HorarioSemanal;

/** El horario del estudiante (RF04, SCRUM-25). El orden por día y hora lo da el dominio. */
public class ConsultarMiHorarioServicio implements ConsultarMiHorario {

	private final HorarioRepositorio horarios;

	public ConsultarMiHorarioServicio(HorarioRepositorio horarios) {
		this.horarios = horarios;
	}

	@Override
	public HorarioSemanal ejecutar(int idEstudiante) {
		return new HorarioSemanal(horarios.clasesDe(idEstudiante));
	}
}
