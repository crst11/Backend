package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * El cifrado de Web Push (RF10, SCRUM-37) contra el ejemplo de la RFC 8291, apéndice A, que da las
 * llaves, la sal y el cuerpo cifrado exactos: si el resultado coincide byte a byte, el cifrado es el del
 * estándar y cualquier navegador sabrá descifrarlo.
 */
class CifradoWebPushTest {

	private static final Base64.Decoder BASE64 = Base64.getUrlDecoder();
	private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();

	// Apéndice A de la RFC 8291.
	private static final String MENSAJE = "When I grow up, I want to be a watermelon";
	private static final String PRIVADA_DEL_SERVIDOR = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
	private static final String PUBLICA_DEL_SERVIDOR =
			"BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
	private static final String PRIVADA_DEL_NAVEGADOR = "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94";
	private static final String PUBLICA_DEL_NAVEGADOR =
			"BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
	private static final String SECRETO = "BTBZMqHH6r4Tts7J_aSIgg";
	private static final String SAL = "DGv6ra1nlYgDCS1FRnbzlw";
	private static final String CUERPO_ESPERADO =
			"DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

	private static KeyPair pareja(String privada, String publica) throws Exception {
		PrivateKey clavePrivada = KeyFactory.getInstance("EC").generatePrivate(
				new ECPrivateKeySpec(new BigInteger(1, BASE64.decode(privada)), CifradoWebPush.parametrosDeLaCurva()));
		return new KeyPair(CifradoWebPush.clavePublica(BASE64.decode(publica)), clavePrivada);
	}

	@Test
	void cifraIgualQueElEjemploDeLaRfc8291() throws Exception {
		byte[] cuerpo = CifradoWebPush.cifrar(MENSAJE.getBytes(StandardCharsets.UTF_8), BASE64.decode(PUBLICA_DEL_NAVEGADOR),
				BASE64.decode(SECRETO), pareja(PRIVADA_DEL_SERVIDOR, PUBLICA_DEL_SERVIDOR), BASE64.decode(SAL));

		assertThat(CODIFICADOR.encodeToString(cuerpo)).isEqualTo(CUERPO_ESPERADO);
	}

	@Test
	void elReceptorDeLaRfcDescifraElCuerpoDelEjemplo() throws Exception {
		// Comprueba el propio ejemplo con un receptor independiente del cifrador: si falla, el que está mal es el vector.
		byte[] mensaje = descifrar(BASE64.decode(CUERPO_ESPERADO), pareja(PRIVADA_DEL_NAVEGADOR, PUBLICA_DEL_NAVEGADOR),
				BASE64.decode(SECRETO));

		assertThat(new String(mensaje, StandardCharsets.UTF_8)).isEqualTo(MENSAJE);
	}

	@Test
	void unCuerpoCifradoConLlavesNuevasLoDescifraElNavegador() throws Exception {
		var navegador = ClavesDePushDePrueba.nueva();
		KeyPair servidor = ClavesDePushDePrueba.nueva().navegador();
		byte[] sal = new byte[16];
		new java.security.SecureRandom().nextBytes(sal);
		byte[] original = "Vence pronto: «Taller de redes» vence hoy a las 11:59 p. m.".getBytes(StandardCharsets.UTF_8);

		byte[] cuerpo = CifradoWebPush.cifrar(original, CifradoWebPush.sinComprimir((ECPublicKey) navegador.navegador().getPublic()),
				navegador.secreto(), servidor, sal);

		assertThat(descifrar(cuerpo, navegador.navegador(), navegador.secreto())).isEqualTo(original);
	}

	@Test
	void laCabeceraLlevaLaSalElTamanoDeRegistroYLaLlaveDelServidor() throws Exception {
		var navegador = ClavesDePushDePrueba.nueva();
		KeyPair servidor = ClavesDePushDePrueba.nueva().navegador();
		byte[] sal = new byte[16];
		Arrays.fill(sal, (byte) 7);

		byte[] cuerpo = CifradoWebPush.cifrar(new byte[] {1}, CifradoWebPush.sinComprimir((ECPublicKey) navegador.navegador().getPublic()),
				navegador.secreto(), servidor, sal);

		ByteBuffer lectura = ByteBuffer.wrap(cuerpo);
		byte[] salLeida = new byte[16];
		lectura.get(salLeida);
		assertThat(salLeida).isEqualTo(sal);
		assertThat(lectura.getInt()).isEqualTo(4096);
		assertThat(lectura.get() & 0xFF).isEqualTo(65);
		byte[] llave = new byte[65];
		lectura.get(llave);
		assertThat(llave).isEqualTo(CifradoWebPush.sinComprimir((ECPublicKey) servidor.getPublic()));
	}

	@Test
	void unMensajeDemasiadoGrandeSeRechazaEnVezDeTruncarse() throws Exception {
		var navegador = ClavesDePushDePrueba.nueva();
		KeyPair servidor = ClavesDePushDePrueba.nueva().navegador();
		byte[] justo = new byte[CifradoWebPush.MAXIMO_DEL_MENSAJE];
		byte[] pasado = new byte[CifradoWebPush.MAXIMO_DEL_MENSAJE + 1];
		byte[] publica = CifradoWebPush.sinComprimir((ECPublicKey) navegador.navegador().getPublic());

		assertThat(CifradoWebPush.cifrar(justo, publica, navegador.secreto(), servidor, new byte[16]).length).isLessThanOrEqualTo(4096 + 86);
		assertThatThrownBy(() -> CifradoWebPush.cifrar(pasado, publica, navegador.secreto(), servidor, new byte[16]))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void unaLlavePublicaFueraDeLaCurvaSeRechaza() {
		byte[] fuera = new byte[65];
		fuera[0] = 0x04;
		fuera[32] = 1;
		fuera[64] = 1;

		assertThatThrownBy(() -> CifradoWebPush.clavePublica(fuera)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("no está en la curva");
		assertThatThrownBy(() -> CifradoWebPush.clavePublica(new byte[10])).isInstanceOf(IllegalArgumentException.class);
	}

	/** El receptor de la RFC 8291, sección 4: lo que hace el navegador al recibir el cuerpo. */
	static byte[] descifrar(byte[] cuerpo, KeyPair navegador, byte[] secreto) throws Exception {
		ByteBuffer lectura = ByteBuffer.wrap(cuerpo);
		byte[] sal = new byte[16];
		lectura.get(sal);
		lectura.getInt();
		byte[] publicaDelServidor = new byte[lectura.get() & 0xFF];
		lectura.get(publicaDelServidor);
		byte[] cifrado = new byte[lectura.remaining()];
		lectura.get(cifrado);

		KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
		acuerdo.init(navegador.getPrivate());
		acuerdo.doPhase(CifradoWebPush.clavePublica(publicaDelServidor), true);
		byte[] compartido = acuerdo.generateSecret();

		byte[] publicaDelNavegador = CifradoWebPush.sinComprimir((ECPublicKey) navegador.getPublic());
		byte[] info = concatenar("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), publicaDelNavegador, publicaDelServidor);
		byte[] material = hmac(hmac(secreto, compartido), concatenar(info, new byte[] {1}));
		byte[] pseudoaleatoria = hmac(sal, material);
		byte[] clave = Arrays.copyOf(hmac(pseudoaleatoria, concatenar("Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), new byte[] {1})), 16);
		byte[] nonce = Arrays.copyOf(hmac(pseudoaleatoria, concatenar("Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), new byte[] {1})), 12);

		Cipher descifrador = Cipher.getInstance("AES/GCM/NoPadding");
		descifrador.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, nonce));
		byte[] relleno = descifrador.doFinal(cifrado);
		int fin = relleno.length;
		while (fin > 0 && relleno[fin - 1] == 0) {
			fin--;
		}
		assertThat(relleno[fin - 1]).as("delimitador del último registro").isEqualTo((byte) 0x02);
		return Arrays.copyOf(relleno, fin - 1);
	}

	private static byte[] hmac(byte[] llave, byte[] datos) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(llave, "HmacSHA256"));
		return mac.doFinal(datos);
	}

	private static byte[] concatenar(byte[]... partes) {
		java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
		for (byte[] parte : partes) {
			salida.writeBytes(parte);
		}
		return salida.toByteArray();
	}
}
