package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.VinculoConGoogle;
import java.time.Instant;
import java.util.Optional;

/** Lo que ve el estudiante en Mi cuenta: si tiene Google vinculado y con qué correo. Nunca el "sub". */
public record VinculoConGoogleDto(boolean vinculada, String correo, Instant fechaVinculacion) {

	public static VinculoConGoogleDto desde(Optional<VinculoConGoogle> vinculo) {
		return vinculo.map(VinculoConGoogleDto::desde).orElse(new VinculoConGoogleDto(false, null, null));
	}

	public static VinculoConGoogleDto desde(VinculoConGoogle vinculo) {
		return new VinculoConGoogleDto(true, vinculo.correo(), vinculo.fechaVinculacion());
	}
}
