package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Instant;

/**
 * La cuenta de Google que el estudiante vinculó para entrar con un toque (SCRUM-48). La identidad de
 * la cuenta sigue siendo el correo institucional: Google es solo otra forma de entrar a ella. El
 * identificador es el "sub" de Google, que no cambia aunque la persona cambie su correo de Google.
 */
public record VinculoConGoogle(String identificador, String correo, Instant fechaVinculacion) {

	public VinculoConGoogle {
		if (identificador == null || identificador.isBlank()) {
			throw new ReglaDeNegocioVioladaException("Falta el identificador de la cuenta de Google");
		}
		if (correo == null || correo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("Falta el correo de la cuenta de Google");
		}
		if (fechaVinculacion == null) {
			throw new ReglaDeNegocioVioladaException("Falta la fecha de vinculación");
		}
	}

	public boolean esDe(String otroIdentificador) {
		return identificador.equals(otroIdentificador);
	}
}
