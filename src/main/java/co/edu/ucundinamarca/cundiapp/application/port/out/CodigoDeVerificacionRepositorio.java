package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.CodigoDeVerificacion;
import co.edu.ucundinamarca.cundiapp.domain.model.PropositoDelCodigo;
import java.util.Optional;

public interface CodigoDeVerificacionRepositorio {

	Optional<CodigoDeVerificacion> buscarDe(int idEstudiante, PropositoDelCodigo proposito);

	/** Un estudiante tiene un solo código por propósito: guardar uno nuevo reemplaza el anterior. */
	void guardar(CodigoDeVerificacion codigo);
}
