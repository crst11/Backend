package co.edu.ucundinamarca.cundiapp;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * Las llaves que un navegador le daría a la aplicación al suscribirse a los avisos (RF10), generadas
 * para las pruebas: una llave pública P-256 de 65 bytes y un secreto de autenticación de 16.
 */
public final class ClavesDePushDePrueba {

	/** La pareja de llaves del «navegador» y su secreto, ya en base64 sin relleno como llegan en la petición. */
	public record Suscripcion(KeyPair navegador, byte[] secreto, String clavePublica, String secretoEnBase64) {
	}

	private ClavesDePushDePrueba() {
	}

	public static Suscripcion nueva() {
		try {
			KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
			generador.initialize(new ECGenParameterSpec("secp256r1"));
			KeyPair navegador = generador.generateKeyPair();
			byte[] secreto = new byte[16];
			new SecureRandom().nextBytes(secreto);
			Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
			return new Suscripcion(navegador, secreto, base64.encodeToString(sinComprimir((ECPublicKey) navegador.getPublic())),
					base64.encodeToString(secreto));
		} catch (java.security.GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	/** El punto de la curva como lo entrega el navegador: 0x04, X y Y de 32 bytes cada una. */
	public static byte[] sinComprimir(ECPublicKey clave) {
		byte[] punto = new byte[65];
		punto[0] = 0x04;
		System.arraycopy(coordenada(clave.getW().getAffineX()), 0, punto, 1, 32);
		System.arraycopy(coordenada(clave.getW().getAffineY()), 0, punto, 33, 32);
		return punto;
	}

	private static byte[] coordenada(BigInteger valor) {
		byte[] bytes = valor.toByteArray();
		byte[] resultado = new byte[32];
		int origen = Math.max(0, bytes.length - 32);
		int largo = Math.min(bytes.length, 32);
		System.arraycopy(bytes, origen, resultado, 32 - largo, largo);
		return resultado;
	}
}
