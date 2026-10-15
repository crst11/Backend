package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.GuardarMiPreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciaDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciaDeHorario;

/**
 * Guarda cómo prefiere el estudiante ver su horario (RF04, SCRUM-26). Las reglas (vista, orden y largo
 * de la sede) las comprueba el dominio al construir la preferencia, antes de llegar aquí.
 */
public class GuardarMiPreferenciaDeHorarioServicio implements GuardarMiPreferenciaDeHorario {

	private final PreferenciaDeHorarioRepositorio preferencias;

	public GuardarMiPreferenciaDeHorarioServicio(PreferenciaDeHorarioRepositorio preferencias) {
		this.preferencias = preferencias;
	}

	@Override
	public PreferenciaDeHorario ejecutar(int idEstudiante, PreferenciaDeHorario preferencia) {
		preferencias.guardar(idEstudiante, preferencia);
		return preferencias.de(idEstudiante);
	}
}
