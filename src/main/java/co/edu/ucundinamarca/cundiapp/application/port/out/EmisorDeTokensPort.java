package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.Estudiante;
import java.time.Duration;
import java.time.Instant;

/** Fabrica los tokens: el de acceso (JWT firmado, de corta vida) y el de refresco (aleatorio y opaco). */
public interface EmisorDeTokensPort {

	record TokenDeAcceso(String valor, Instant expira) {
	}

	/**
	 * @param consecutivoSesion la sesión a la que pertenece el token. Viaja dentro como «sid» para
	 *     que Mis sesiones pueda marcar cuál es el dispositivo desde el que se está mirando
	 *     (SCRUM-73). No es un dato sensible: identifica una sesión del propio estudiante.
	 */
	TokenDeAcceso emitirAcceso(Estudiante estudiante, int consecutivoSesion, Instant ahora, Duration vigencia);

	String generarTokenDeRefresco();
}
