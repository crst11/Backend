package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciaDeHorario;

/**
 * Guardar cómo prefiere el estudiante ver su horario (RF04, SCRUM-26). Reemplaza la anterior: es una
 * sola preferencia por estudiante, no una lista.
 */
public interface GuardarMiPreferenciaDeHorario {

	PreferenciaDeHorario ejecutar(int idEstudiante, PreferenciaDeHorario preferencia);
}
