package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
import java.util.Optional;

/** Caso de uso: saber si la cuenta tiene Google vinculado y con qué correo, para mostrarlo en Mi cuenta. */
public interface ConsultarVinculoConGoogle {

	Optional<VinculoConGoogle> ejecutar(int idEstudiante);
}
