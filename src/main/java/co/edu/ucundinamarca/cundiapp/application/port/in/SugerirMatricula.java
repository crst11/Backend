package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Qué materias matricular el próximo semestre, según mi plan y lo que llevo cursado (RF13, SCRUM-244). */
public interface SugerirMatricula {

	SugerenciaParaMatricular ejecutar(int idEstudiante);
}
