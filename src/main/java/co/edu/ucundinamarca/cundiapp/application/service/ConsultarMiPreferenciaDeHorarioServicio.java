package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiPreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciaDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciaDeHorario;

/** Cómo prefiere el estudiante ver su horario (RF04, SCRUM-26). */
public class ConsultarMiPreferenciaDeHorarioServicio implements ConsultarMiPreferenciaDeHorario {

	private final PreferenciaDeHorarioRepositorio preferencias;

	public ConsultarMiPreferenciaDeHorarioServicio(PreferenciaDeHorarioRepositorio preferencias) {
		this.preferencias = preferencias;
	}

	@Override
	public PreferenciaDeHorario ejecutar(int idEstudiante) {
		return preferencias.de(idEstudiante);
	}
}
