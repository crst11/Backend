package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.CodigoDeVerificacion;
import java.util.Optional;

public interface CodigoDeVerificacionRepositorio {

	Optional<CodigoDeVerificacion> buscarDe(int idEstudiante);

	/** Un estudiante tiene un solo código: guardar uno nuevo reemplaza el anterior. */
	void guardar(CodigoDeVerificacion codigo);
}
