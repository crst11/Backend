package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;

/** Los ajustes propios del estudiante (RF08, SCRUM-34). Por ahora, su meta de calificación. */
public interface ConsultarMiConfiguracion {

	Configuracion ejecutar(int idEstudiante);

	record Configuracion(Meta meta, UmbralesDeRiesgo umbrales) {
	}
}
