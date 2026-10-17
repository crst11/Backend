package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Meta;

/** Cambia la nota a la que apunta el estudiante (RF08, SCRUM-34). Vale para todas sus asignaturas. */
public interface CambiarMiMeta {

	ConsultarMiConfiguracion.Configuracion ejecutar(int idEstudiante, Meta meta);
}
