package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.Pendiente;
import java.util.List;
import java.util.Optional;

/**
 * Dónde viven los pendientes del estudiante (RF06). Todo se pide con el estudiante: los pendientes son
 * suyos y uno ajeno se comporta como si no existiera.
 */
public interface PendienteRepositorio {

	/** Guarda uno nuevo (sin número) o los cambios de uno que ya existe; lo devuelve con su número. */
	Pendiente guardar(Pendiente pendiente);

	/** Los del estudiante; sin los cumplidos si no se piden. El orden lo pone el dominio. */
	List<Pendiente> listar(int idEstudiante, boolean incluirCumplidos);

	Optional<Pendiente> buscar(int idEstudiante, int idPendiente);

	/** Verdadero si existía y era del estudiante. */
	boolean eliminar(int idEstudiante, int idPendiente);
}
