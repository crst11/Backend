package co.edu.ucundinamarca.cundiapp.application.port.in;

/** Marca un aviso mío como leído, o lo devuelve a sin leer (RF10, SCRUM-37). Uno ajeno responde como si no existiera. */
public interface MarcarMiAvisoComoLeido {

	void ejecutar(int idEstudiante, int idAviso, boolean leido);
}
