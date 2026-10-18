package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciaDeHorario;

/** Cómo prefiere el estudiante ver su horario (RF04, SCRUM-26). Sin nada guardado, la predeterminada. */
public interface ConsultarMiPreferenciaDeHorario {

	PreferenciaDeHorario ejecutar(int idEstudiante);
}
