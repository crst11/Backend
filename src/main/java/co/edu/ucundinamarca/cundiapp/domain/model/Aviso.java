package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.Instant;

/**
 * Un aviso para el estudiante (RF10, SCRUM-37): lo que se le dice, de qué tipo es y qué pasó con su
 * entrega a los dispositivos.
 *
 * <p>El aviso existe y se ve dentro de la aplicación aunque nunca llegue a un dispositivo: la entrega
 * por Web Push es un extra, no una condición. Por eso {@code estadoEnvio} y {@code leido} son cosas
 * distintas.
 */
public record Aviso(
		Integer id,
		int idEstudiante,
		TipoDeAviso tipo,
		String titulo,
		String mensaje,
		Instant fechaGeneracion,
		EstadoDeEnvio estadoEnvio,
		boolean leido,
		int intentos) {

	/** Cuántas veces se intenta entregar un aviso antes de darlo por fallido. La base de datos admite hasta cinco. */
	public static final int MAXIMO_DE_INTENTOS = 3;

	private static final int LARGO_MAXIMO_DEL_TITULO = 120;

	public Aviso {
		if (tipo == null || estadoEnvio == null || fechaGeneracion == null) {
			throw new ReglaDeNegocioVioladaException("El aviso necesita tipo, estado y fecha");
		}
		if (titulo == null || titulo.isBlank() || titulo.length() > LARGO_MAXIMO_DEL_TITULO) {
			throw new ReglaDeNegocioVioladaException("El aviso necesita un título de hasta 120 caracteres");
		}
		if (mensaje == null || mensaje.isBlank()) {
			throw new ReglaDeNegocioVioladaException("El aviso necesita un mensaje");
		}
		if (intentos < 0 || intentos > MAXIMO_DE_INTENTOS) {
			throw new ReglaDeNegocioVioladaException("Los intentos de entrega no son válidos");
		}
	}

	/** Un aviso recién generado: sin leer y sin entregar. */
	public static Aviso nuevo(int idEstudiante, TipoDeAviso tipo, String titulo, String mensaje, Instant ahora) {
		return new Aviso(null, idEstudiante, tipo, titulo, mensaje, ahora, EstadoDeEnvio.PENDIENTE, false, 0);
	}
}
