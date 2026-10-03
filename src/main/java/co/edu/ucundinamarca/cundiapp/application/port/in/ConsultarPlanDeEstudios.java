package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;

/** Caso de uso: ver la ruta de aprendizaje de un programa, período por período (RF02). */
public interface ConsultarPlanDeEstudios {

	PlanDeEstudios ejecutar(String codigoPrograma);
}
