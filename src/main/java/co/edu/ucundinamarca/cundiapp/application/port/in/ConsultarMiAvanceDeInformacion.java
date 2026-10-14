package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AvanceDeInformacion;

/**
 * Cuánta información lleva cargada el estudiante y qué le falta (RF03, SCRUM-84): lo que muestra
 * «Importar todos mis archivos».
 */
public interface ConsultarMiAvanceDeInformacion {

	AvanceDeInformacion ejecutar(int idEstudiante);
}
