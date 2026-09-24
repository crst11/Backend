package co.edu.ucundinamarca.cundiapp.dominio.modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Código de 6 dígitos que confirma que el correo institucional es del estudiante (RF01).
 * Vence a los 15 minutos y admite 5 intentos fallidos. Solo se conserva su huella SHA-256.
 */
public record CodigoDeVerificacion(
		int idEstudiante,
		String huella,
		Instant fechaEmision,
		Instant fechaExpiracion,
		int intentosFallidos,
		Instant fechaUso) {

	public static final Duration VIGENCIA = Duration.ofMinutes(15);
	public static final int MAXIMO_INTENTOS = 5;

	public enum Resultado { ACEPTADO, INCORRECTO, VENCIDO, INTENTOS_AGOTADOS, YA_USADO }

	/** El código actualizado que hay que guardar y cómo terminó el intento. */
	public record Intento(CodigoDeVerificacion codigo, Resultado resultado) {
	}

	public static CodigoDeVerificacion emitir(int idEstudiante, String codigoEnClaro, Instant ahora) {
		return new CodigoDeVerificacion(idEstudiante, huellaDe(codigoEnClaro), ahora, ahora.plus(VIGENCIA), 0, null);
	}

	public Intento intentar(String codigoEnClaro, Instant ahora) {
		if (fechaUso != null) {
			return new Intento(this, Resultado.YA_USADO);
		}
		if (ahora.isAfter(fechaExpiracion)) {
			return new Intento(this, Resultado.VENCIDO);
		}
		if (intentosFallidos >= MAXIMO_INTENTOS) {
			return new Intento(this, Resultado.INTENTOS_AGOTADOS);
		}
		if (coincide(codigoEnClaro)) {
			return new Intento(
					new CodigoDeVerificacion(idEstudiante, huella, fechaEmision, fechaExpiracion, intentosFallidos, ahora),
					Resultado.ACEPTADO);
		}
		return new Intento(
				new CodigoDeVerificacion(idEstudiante, huella, fechaEmision, fechaExpiracion, intentosFallidos + 1, null),
				Resultado.INCORRECTO);
	}

	public int intentosRestantes() {
		return MAXIMO_INTENTOS - intentosFallidos;
	}

	/** Se puede pedir uno nuevo cuando el anterior venció o ya no admite más intentos. */
	public boolean puedeReemitirse(Instant ahora) {
		return fechaUso == null && (ahora.isAfter(fechaExpiracion) || intentosFallidos >= MAXIMO_INTENTOS);
	}

	private boolean coincide(String codigoEnClaro) {
		return MessageDigest.isEqual(
				huella.getBytes(StandardCharsets.UTF_8), huellaDe(codigoEnClaro).getBytes(StandardCharsets.UTF_8));
	}

	static String huellaDe(String codigoEnClaro) {
		try {
			byte[] resumen = MessageDigest.getInstance("SHA-256").digest(codigoEnClaro.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumen);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no está disponible en esta JVM", e);
		}
	}
}
