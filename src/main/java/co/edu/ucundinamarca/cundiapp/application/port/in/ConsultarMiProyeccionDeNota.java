package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Meta;

/** La nota que hace falta en una asignatura para llegar a la meta (RF08, SCRUM-33). */
public interface ConsultarMiProyeccionDeNota {

	/** @param meta la meta a probar; {@code null} usa la guardada del estudiante, sin cambiársela */
	SimularMiNota.Resultado ejecutar(int idEstudiante, int idMatricula, Meta meta);
}
