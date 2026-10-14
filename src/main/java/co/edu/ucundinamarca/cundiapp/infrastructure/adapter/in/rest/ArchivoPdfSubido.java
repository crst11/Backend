package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.multipart.MultipartFile;

/**
 * Un PDF que sube el estudiante, revisado antes de que PDFBox lo abra (SCRUM-89). Es el único lugar
 * donde se comprueba: el Registro Académico Extendido y el Reporte de Horario lo usan los dos.
 *
 * <p>No basta con que el nombre termine en `.pdf`: cualquier archivo se puede renombrar. Un PDF de
 * verdad empieza por `%PDF-`, y lo demás (cifrado, páginas, scripts) se revisa ya abierto en
 * {@code ValidacionDePdf}.
 */
final class ArchivoPdfSubido {

	static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
	private static final byte[] FIRMA = "%PDF-".getBytes(StandardCharsets.US_ASCII);

	private ArchivoPdfSubido() {
	}

	/** Devuelve el contenido si es un PDF razonable; si no, explica por qué en un 422. */
	static byte[] leer(MultipartFile archivo, String nombreDelReporte) {
		if (archivo == null || archivo.isEmpty()) {
			throw new ReglaDeNegocioVioladaException("Falta el archivo");
		}
		if (archivo.getSize() > TAMANO_MAXIMO) {
			throw new ReglaDeNegocioVioladaException(
					"El archivo pesa más de 5 MB; el %s pesa mucho menos. Revisa que sea el reporte correcto"
							.formatted(nombreDelReporte));
		}
		String nombre = archivo.getOriginalFilename();
		if (nombre == null || !nombre.toLowerCase().endsWith(".pdf")) {
			throw new ReglaDeNegocioVioladaException("El reporte tiene que ser el archivo PDF que descargaste");
		}
		byte[] contenido;
		try {
			contenido = archivo.getBytes();
		} catch (IOException error) {
			throw new ReglaDeNegocioVioladaException("No pudimos leer el archivo que subiste");
		}
		if (!empiezaPorFirmaDePdf(contenido)) {
			throw new ReglaDeNegocioVioladaException("El reporte tiene que ser el archivo PDF que descargaste");
		}
		return contenido;
	}

	private static boolean empiezaPorFirmaDePdf(byte[] contenido) {
		if (contenido.length < FIRMA.length) {
			return false;
		}
		for (int i = 0; i < FIRMA.length; i++) {
			if (contenido[i] != FIRMA[i]) {
				return false;
			}
		}
		return true;
	}
}
