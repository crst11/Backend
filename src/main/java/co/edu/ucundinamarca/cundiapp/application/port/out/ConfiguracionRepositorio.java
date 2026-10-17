package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;

/** Lo que el núcleo necesita para leer y guardar los ajustes del estudiante (RF08, SCRUM-34). */
public interface ConfiguracionRepositorio {

	/** La meta guardada, o {@link Meta#PREDETERMINADA} si el estudiante nunca la cambió. */
	Meta metaDe(int idEstudiante);

	void guardarMeta(int idEstudiante, Meta meta);

	/** Los umbrales guardados, o {@link UmbralesDeRiesgo#PREDETERMINADOS} si el estudiante nunca los cambió. */
	UmbralesDeRiesgo umbralesDe(int idEstudiante);

	void guardarUmbrales(int idEstudiante, UmbralesDeRiesgo umbrales);
}
