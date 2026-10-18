package co.edu.ucundinamarca.cundiapp.application.port.in;

/**
 * Deja de mandar avisos a uno de mis dispositivos (RF10, SCRUM-38). Uno ajeno, o que ya no recibe avisos,
 * responde como si no existiera.
 */
public interface QuitarMiDispositivoConAvisos {

	void ejecutar(int idEstudiante, int consecutivo);
}
