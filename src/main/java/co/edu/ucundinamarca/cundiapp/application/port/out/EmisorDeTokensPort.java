package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Duration;
import java.time.Instant;

/** Fabrica los tokens: el de acceso (JWT firmado, de corta vida) y el de refresco (aleatorio y opaco). */
public interface EmisorDeTokensPort {

	record TokenDeAcceso(String valor, Instant expira) {
	}

	TokenDeAcceso emitirAcceso(Estudiante estudiante, Instant ahora, Duration vigencia);

	String generarTokenDeRefresco();
}
