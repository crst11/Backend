package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Quitar un bloque del horario (RF04, SCRUM-25). Quitar uno que ya no está se rechaza. */
public interface QuitarClaseDelHorario {

	void ejecutar(int idEstudiante, int idMatricula, int consecutivoDelBloque);
}
