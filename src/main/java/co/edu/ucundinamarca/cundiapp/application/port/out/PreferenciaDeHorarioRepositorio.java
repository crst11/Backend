package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciaDeHorario;

/** Lo que el núcleo necesita para guardar y leer la preferencia de horario (RF04, SCRUM-26). */
public interface PreferenciaDeHorarioRepositorio {

	/** Lo guardado, o la predeterminada si el estudiante nunca eligió nada. */
	PreferenciaDeHorario de(int idEstudiante);

	void guardar(int idEstudiante, PreferenciaDeHorario preferencia);
}
