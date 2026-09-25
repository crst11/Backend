package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import java.util.Optional;

public interface CodigoDeVerificacionRepositorio {

	Optional<CodigoDeVerificacion> buscarDe(int idEstudiante);

	/** Un estudiante tiene un solo código: guardar uno nuevo reemplaza el anterior. */
	void guardar(CodigoDeVerificacion codigo);
}
