package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Un documento, plantilla o página oficial de la universidad que la guía institucional enlaza
 * (RF11). CundiApp no guarda copias: el recurso siempre apunta a su fuente oficial.
 */
public record RecursoInstitucional(
		Integer id,
		CategoriaDeRecurso categoria,
		String titulo,
		String descripcion,
		String url,
		TipoDeRecurso tipo,
		EstadoDeRecurso estado,
		LocalDate fechaVerificacion) {

	public RecursoInstitucional {
		if (categoria == null) {
			throw new ReglaDeNegocioVioladaException("El recurso necesita una categoría");
		}
		if (titulo == null || titulo.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El recurso necesita un título");
		}
		if (url == null || !url.startsWith("https://")) {
			throw new ReglaDeNegocioVioladaException("El recurso debe enlazar a su fuente oficial por https");
		}
		if (tipo == null || estado == null) {
			throw new ReglaDeNegocioVioladaException("El recurso necesita tipo y estado");
		}
	}

	public boolean estaVigente() {
		return estado == EstadoDeRecurso.VIGENTE;
	}

	/**
	 * Busca como lo haría una persona: sin distinguir mayúsculas ni tildes, con todas las palabras
	 * escritas (en cualquier orden) y aceptando singular o plural ("plantillas" encuentra "Plantilla").
	 * Se busca en el título, la descripción y el nombre de la categoría. Una búsqueda vacía coincide con todo.
	 */
	public boolean coincideCon(String busqueda) {
		if (busqueda == null || busqueda.isBlank()) {
			return true;
		}
		String texto = normalizar(titulo + " " + (descripcion == null ? "" : descripcion) + " " + categoria.nombre());
		return Arrays.stream(normalizar(busqueda).split(" "))
				.filter(palabra -> !palabra.isBlank())
				.allMatch(palabra -> contienePalabra(texto, palabra));
	}

	/** Si el enlace lleva a un archivo, su formato para mostrarlo ("PDF", "Word"...); si es una página, vacío. */
	public Optional<String> formatoDeArchivo() {
		String ruta = URI.create(url).getPath().toLowerCase(Locale.ROOT);
		if (ruta.endsWith(".pdf")) {
			return Optional.of("PDF");
		}
		if (ruta.endsWith(".docx") || ruta.endsWith(".doc")) {
			return Optional.of("Word");
		}
		if (ruta.endsWith(".xlsx") || ruta.endsWith(".xls")) {
			return Optional.of("Excel");
		}
		if (ruta.endsWith(".pptx") || ruta.endsWith(".ppt")) {
			return Optional.of("PowerPoint");
		}
		return Optional.empty();
	}

	private static boolean contienePalabra(String texto, String palabra) {
		if (texto.contains(palabra)) {
			return true;
		}
		// Plural sencillo del español: "materias" → "materia", "trámites" → "tramite", "reglamentos" → "reglamento".
		if (palabra.length() > 4 && palabra.endsWith("es") && texto.contains(palabra.substring(0, palabra.length() - 2))) {
			return true;
		}
		return palabra.length() > 3 && palabra.endsWith("s") && texto.contains(palabra.substring(0, palabra.length() - 1));
	}

	static String normalizar(String texto) {
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}
}
