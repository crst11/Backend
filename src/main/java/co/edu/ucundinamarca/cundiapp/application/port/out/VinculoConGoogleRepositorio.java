package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
import java.time.Instant;
import java.util.Optional;

public interface VinculoConGoogleRepositorio {

	/** El estudiante que vinculó la cuenta de Google con ese identificador, si alguno lo hizo. */
	Optional<Integer> estudianteVinculadoA(String identificador);

	Optional<VinculoConGoogle> buscarDe(int idEstudiante);

	void vincular(int idEstudiante, VinculoConGoogle vinculo);

	/** Quita el vínculo si existe; la contraseña sigue siendo un método de acceso. */
	void desvincular(int idEstudiante);

	void registrarAcceso(int idEstudiante, Instant fecha);
}
