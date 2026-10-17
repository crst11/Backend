package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;

/** Cambia los umbrales de riesgo del estudiante (RF09, SCRUM-36). La Brújula se recalcula en la siguiente consulta. */
public interface CambiarMisUmbrales {

	ConsultarMiConfiguracion.Configuracion ejecutar(int idEstudiante, UmbralesDeRiesgo umbrales);
}
