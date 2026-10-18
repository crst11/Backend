package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * El cifrado de un mensaje Web Push: RFC 8291 con la codificación {@code aes128gcm} de la RFC 8188.
 *
 * <p>Solo usa lo que trae el JDK (ECDH sobre P-256, HKDF con HMAC-SHA-256 y AES-128-GCM), sin
 * bibliotecas de terceros. Se prueba contra el ejemplo de la propia RFC 8291, apéndice A, que da las
 * llaves, la sal y el mensaje cifrado exactos.
 *
 * <p>Un mensaje cabe en un solo registro de 4096 bytes: es lo máximo que aceptan los servicios de
 * avisos. Los nuestros son títulos y frases cortas.
 */
final class CifradoWebPush {

	static final int TAMANO_DEL_REGISTRO = 4096;
	static final int LARGO_DE_LA_SAL = 16;
	static final int LARGO_DE_LA_CLAVE_PUBLICA = 65;
	/** Cabecera del cuerpo: sal, tamaño de registro, largo del identificador y la llave pública del servidor. */
	private static final int LARGO_DE_LA_CABECERA = LARGO_DE_LA_SAL + 4 + 1 + LARGO_DE_LA_CLAVE_PUBLICA;
	private static final int LARGO_DE_LA_ETIQUETA = 16;
	/** Tras el mensaje va un byte de relleno que marca el último registro (0x02). */
	private static final int BYTES_DE_RELLENO = 1;
	static final int MAXIMO_DEL_MENSAJE =
			TAMANO_DEL_REGISTRO - LARGO_DE_LA_CABECERA - LARGO_DE_LA_ETIQUETA - BYTES_DE_RELLENO;

	private CifradoWebPush() {
	}

	/**
	 * @param mensaje lo que se cifra
	 * @param clavePublicaDelNavegador su llave pública, 65 bytes (punto sin comprimir de P-256)
	 * @param secretoDeAutenticacion el secreto de 16 bytes que el navegador comparte con la aplicación
	 * @param claveDelServidor la pareja de llaves de este mensaje: debe ser efímera, una por mensaje
	 * @param sal 16 bytes aleatorios, distintos en cada mensaje
	 * @return el cuerpo de la petición, cabecera incluida
	 */
	static byte[] cifrar(
			byte[] mensaje,
			byte[] clavePublicaDelNavegador,
			byte[] secretoDeAutenticacion,
			KeyPair claveDelServidor,
			byte[] sal) {
		if (mensaje.length > MAXIMO_DEL_MENSAJE) {
			throw new IllegalArgumentException("El mensaje no cabe en un registro de Web Push");
		}
		if (sal.length != LARGO_DE_LA_SAL) {
			throw new IllegalArgumentException("La sal tiene que ser de 16 bytes");
		}
		try {
			byte[] publicaDelServidor = sinComprimir((ECPublicKey) claveDelServidor.getPublic());
			byte[] secretoCompartido = acordar(claveDelServidor.getPrivate(), clavePublicaDelNavegador);

			byte[] claveBase = hmac(secretoDeAutenticacion, secretoCompartido);
			byte[] infoDeLaClave = concatenar(
					"WebPush: info\0".getBytes(StandardCharsets.US_ASCII), clavePublicaDelNavegador, publicaDelServidor);
			byte[] materialDeLaClave = expandir(claveBase, infoDeLaClave, 32);

			byte[] pseudoaleatoria = hmac(sal, materialDeLaClave);
			byte[] claveDeContenido = expandir(
					pseudoaleatoria, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
			byte[] nonce = expandir(pseudoaleatoria, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

			Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
			cifrador.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(claveDeContenido, "AES"),
					new GCMParameterSpec(LARGO_DE_LA_ETIQUETA * 8, nonce));
			byte[] cifrado = cifrador.doFinal(concatenar(mensaje, new byte[] {0x02}));

			byte[] cabecera = ByteBuffer.allocate(LARGO_DE_LA_CABECERA)
					.put(sal)
					.putInt(TAMANO_DEL_REGISTRO)
					.put((byte) LARGO_DE_LA_CLAVE_PUBLICA)
					.put(publicaDelServidor)
					.array();
			return concatenar(cabecera, cifrado);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo cifrar el aviso", e);
		}
	}

	/** El punto de la curva como lo entrega el navegador: 0x04, y luego X e Y de 32 bytes cada una. */
	static byte[] sinComprimir(ECPublicKey clave) {
		byte[] punto = new byte[LARGO_DE_LA_CLAVE_PUBLICA];
		punto[0] = 0x04;
		System.arraycopy(coordenada(clave.getW().getAffineX()), 0, punto, 1, 32);
		System.arraycopy(coordenada(clave.getW().getAffineY()), 0, punto, 33, 32);
		return punto;
	}

	/**
	 * La llave pública de un navegador a partir de sus 65 bytes. Se comprueba que el punto esté de
	 * verdad en la curva P-256: un punto fuera de ella no es una llave, es un ataque.
	 */
	static ECPublicKey clavePublica(byte[] punto) throws GeneralSecurityException {
		if (punto.length != LARGO_DE_LA_CLAVE_PUBLICA || punto[0] != 0x04) {
			throw new IllegalArgumentException("La llave pública no es un punto sin comprimir de P-256");
		}
		BigInteger x = new BigInteger(1, Arrays.copyOfRange(punto, 1, 33));
		BigInteger y = new BigInteger(1, Arrays.copyOfRange(punto, 33, 65));
		ECParameterSpec parametros = parametrosDeLaCurva();
		if (!estaEnLaCurva(x, y, parametros)) {
			throw new IllegalArgumentException("La llave pública no está en la curva P-256");
		}
		return (ECPublicKey) KeyFactory.getInstance("EC")
				.generatePublic(new ECPublicKeySpec(new ECPoint(x, y), parametros));
	}

	static ECParameterSpec parametrosDeLaCurva() throws GeneralSecurityException {
		AlgorithmParameters parametros = AlgorithmParameters.getInstance("EC");
		parametros.init(new ECGenParameterSpec("secp256r1"));
		return parametros.getParameterSpec(ECParameterSpec.class);
	}

	private static boolean estaEnLaCurva(BigInteger x, BigInteger y, ECParameterSpec parametros) {
		BigInteger primo = ((ECFieldFp) parametros.getCurve().getField()).getP();
		if (x.signum() < 0 || x.compareTo(primo) >= 0 || y.signum() < 0 || y.compareTo(primo) >= 0) {
			return false;
		}
		BigInteger izquierda = y.multiply(y).mod(primo);
		BigInteger derecha = x.pow(3)
				.add(parametros.getCurve().getA().multiply(x))
				.add(parametros.getCurve().getB())
				.mod(primo);
		return izquierda.equals(derecha);
	}

	private static byte[] acordar(PrivateKey privada, byte[] clavePublicaDelNavegador) throws GeneralSecurityException {
		KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
		acuerdo.init(privada);
		acuerdo.doPhase(clavePublica(clavePublicaDelNavegador), true);
		return acuerdo.generateSecret();
	}

	private static byte[] hmac(byte[] llave, byte[] datos) throws GeneralSecurityException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(llave, "HmacSHA256"));
		return mac.doFinal(datos);
	}

	/** HKDF-Expand de la RFC 5869 para a lo sumo 32 bytes: un solo bloque, T(1) = HMAC(PRK, info || 0x01). */
	private static byte[] expandir(byte[] pseudoaleatoria, byte[] info, int largo) throws GeneralSecurityException {
		return Arrays.copyOf(hmac(pseudoaleatoria, concatenar(info, new byte[] {0x01})), largo);
	}

	private static byte[] coordenada(BigInteger valor) {
		byte[] bytes = valor.toByteArray();
		byte[] resultado = new byte[32];
		int origen = Math.max(0, bytes.length - 32);
		int largo = Math.min(bytes.length, 32);
		System.arraycopy(bytes, origen, resultado, 32 - largo, largo);
		return resultado;
	}

	private static byte[] concatenar(byte[]... partes) {
		int total = 0;
		for (byte[] parte : partes) {
			total += parte.length;
		}
		byte[] resultado = new byte[total];
		int posicion = 0;
		for (byte[] parte : partes) {
			System.arraycopy(parte, 0, resultado, posicion, parte.length);
			posicion += parte.length;
		}
		return resultado;
	}
}
