package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Sesion;
import java.util.List;

/** Caso de uso: ver desde qué dispositivos hay una sesión abierta (RF01). */
public interface ListarMisSesiones {

	List<Sesion> ejecutar(int idEstudiante);
}
