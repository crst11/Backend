package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PlanDeEstudios;
import java.util.Optional;

/** Lo que el estudiante eligió como su programa, y el plan que le corresponde. Vacío si aún no elige. */
public record MiPerfilAcademico(Optional<PlanDeEstudios> plan) {

	public static MiPerfilAcademico sinElegir() {
		return new MiPerfilAcademico(Optional.empty());
	}
}
