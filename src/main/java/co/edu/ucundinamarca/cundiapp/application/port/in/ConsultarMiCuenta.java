package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;

/** Caso de uso: el estudiante autenticado consulta su propia cuenta. El id sale del token, nunca de la ruta. */
public interface ConsultarMiCuenta {

	Estudiante ejecutar(int idEstudiante);
}
