package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Meta;

/** Los ajustes propios del estudiante (RF08, SCRUM-34). Por ahora, su meta de calificación. */
public interface ConsultarMiConfiguracion {

	Configuracion ejecutar(int idEstudiante);

	/** Cuando lleguen los umbrales de riesgo (RF09) se agregan aquí. */
	record Configuracion(Meta meta) {
	}
}
