package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConSesion;
import java.util.List;

/** Caso de uso: ver desde qué dispositivos hay una sesión abierta (RF01). */
public interface ListarMisSesiones {

	/**
	 * Un dispositivo por cadena de rotaciones, no una línea por fila de la tabla (SCRUM-73).
	 *
	 * @param sesionActual el consecutivo que trae el token de quien pregunta, o nulo si no lo trae.
	 */
	List<DispositivoConSesion> ejecutar(int idEstudiante, Integer sesionActual);
}
