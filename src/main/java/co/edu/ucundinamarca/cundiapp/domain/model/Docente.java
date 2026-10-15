package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Quién dicta una clase (RF04, SCRUM-25). Solo nombres y apellidos: nunca el documento de
 * identidad de un docente (Ley 1581 de 2012).
 *
 * <p>Se identifica por su nombre normalizado, sin tildes, sin mayúsculas y sin espacios de más. Así
 * «María Pérez» y «maria perez» son la misma persona aunque dos estudiantes lo escriban distinto, y
 * no se crea un docente nuevo por cada variante.
 */
public record Docente(String nombres, String apellidos) {

	/** Lo que cabe en docente.nombres y docente.apellidos. */
	static final int LARGO_MAXIMO = 80;

	public Docente {
		nombres = limpio(nombres, "Faltan los nombres del docente");
		apellidos = limpio(apellidos, "Faltan los apellidos del docente");
		if (nombres.length() > LARGO_MAXIMO || apellidos.length() > LARGO_MAXIMO) {
			throw new ReglaDeNegocioVioladaException(
					"Los nombres y los apellidos del docente no pueden pasar de %d caracteres cada uno"
							.formatted(LARGO_MAXIMO));
		}
	}

	public String nombreNormalizado() {
		String completo = nombres + " " + apellidos;
		String sinTildes = Normalizer.normalize(completo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
	}

	private static String limpio(String texto, String siFalta) {
		if (texto == null || texto.isBlank()) {
			throw new ReglaDeNegocioVioladaException(siFalta);
		}
		return texto.strip().replaceAll("\\s+", " ");
	}
}
