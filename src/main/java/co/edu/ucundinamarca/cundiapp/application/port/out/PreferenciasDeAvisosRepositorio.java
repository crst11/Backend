package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;

/** Lo que el núcleo necesita para leer y guardar las preferencias de avisos del estudiante (RF10, SCRUM-38). */
public interface PreferenciasDeAvisosRepositorio {

	/** Las guardadas, o {@link PreferenciasDeAvisos#PREDETERMINADAS} si el estudiante nunca las cambió. */
	PreferenciasDeAvisos de(int idEstudiante);

	void guardar(int idEstudiante, PreferenciasDeAvisos preferencias);
}
