package co.edu.ucundinamarca.cundiapp.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Una sesión abierta con contraseña o con Google (RF01). Cada token de refresco vale una sola vez: al
 * usarlo se revoca por rotación y nace otra sesión con el mismo método. De ese token solo se conserva
 * la huella SHA-256.
 */
public record Sesion(
		int idEstudiante,
		Integer consecutivo,
		MetodoDeAcceso metodo,
		String huellaRefresco,
		Instant fechaInicio,
		Instant fechaExpiracion,
		Instant fechaRevocacion,
		MotivoDeRevocacion motivoRevocacion,
		String userAgent,
		String ipOrigen) {

	public static Sesion abrir(
			int idEstudiante,
			MetodoDeAcceso metodo,
			String tokenDeRefresco,
			Instant ahora,
			Duration vigencia,
			String userAgent,
			String ip) {
		return new Sesion(idEstudiante, null, metodo, huellaDe(tokenDeRefresco), ahora, ahora.plus(vigencia), null, null,
				userAgent, ip);
	}

	public boolean estaRevocada() {
		return fechaRevocacion != null;
	}

	public boolean estaVigente(Instant ahora) {
		return !estaRevocada() && ahora.isBefore(fechaExpiracion);
	}

	public boolean fueRotada() {
		return motivoRevocacion == MotivoDeRevocacion.ROTACION;
	}

	public Sesion revocar(MotivoDeRevocacion motivo, Instant ahora) {
		return new Sesion(idEstudiante, consecutivo, metodo, huellaRefresco, fechaInicio, fechaExpiracion, ahora, motivo,
				userAgent, ipOrigen);
	}

	public static String huellaDe(String tokenDeRefresco) {
		try {
			byte[] resumen = MessageDigest.getInstance("SHA-256")
					.digest(tokenDeRefresco.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumen);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no está disponible en esta JVM", e);
		}
	}
}
