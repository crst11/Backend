package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

import co.edu.ucundinamarca.cundiapp.dominio.modelo.Estudiante;
import java.time.Duration;
import java.time.Instant;

/** Fabrica los tokens: el de acceso (JWT firmado, de corta vida) y el de refresco (aleatorio y opaco). */
public interface EmisorDeTokensPort {

	record TokenDeAcceso(String valor, Instant expira) {
	}

	TokenDeAcceso emitirAcceso(Estudiante estudiante, Instant ahora, Duration vigencia);

	String generarTokenDeRefresco();
}
