package co.edu.ucundinamarca.cundiapp.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Cómo se comparan los textos que escribe una persona: sin tildes, sin mayúsculas y sin espacios de
 * más. «Fusagasugá», «fusagasuga» y « FUSAGASUGA » son la misma sede.
 */
final class TextoNormalizado {

	private TextoNormalizado() {
	}

	static String de(String texto) {
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
	}
}
