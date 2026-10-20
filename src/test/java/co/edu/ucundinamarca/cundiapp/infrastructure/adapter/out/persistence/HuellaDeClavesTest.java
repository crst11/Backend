package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * La clave con que se guarda un límite no deja ver lo que se cuenta (12 factores, factor 6, SCRUM-216): un HMAC de 64
 * caracteres que solo se puede calcular con el secreto del servidor.
 */
class HuellaDeClavesTest {

	private final HuellaDeClaves huella = new HuellaDeClaves("secreto-de-prueba-0123456789");

	@Test
	void sonSesentaYCuatroCaracteresHexadecimalesYSiempreLosMismos() {
		String clave = huella.de("intentos", "correo:ana@ucundinamarca.edu.co");

		assertThat(clave).matches("[0-9a-f]{64}");
		assertThat(huella.de("intentos", "correo:ana@ucundinamarca.edu.co")).isEqualTo(clave);
	}

	@Test
	void noContieneNiElCorreoNiLaIp() {
		assertThat(huella.de("intentos", "correo:ana@ucundinamarca.edu.co")).doesNotContain("ana").doesNotContain("ucundinamarca");
		assertThat(huella.de("correos-por-ip", "10.20.30.40")).doesNotContain("30.40");
	}

	@Test
	void laMismaClaveEnDosLimitesDistintosDaDosHuellas() {
		assertThat(huella.de("intentos", "ip:10.0.0.1")).isNotEqualTo(huella.de("peticiones", "ip:10.0.0.1"));
	}

	@Test
	void dosClavesDistintasDanDosHuellas() {
		assertThat(huella.de("intentos", "ip:10.0.0.1")).isNotEqualTo(huella.de("intentos", "ip:10.0.0.2"));
	}

	@Test
	void sinElSecretoDelServidorNoSePuedeRepetirElCalculo() {
		// Con otro secreto la misma IP da otra huella: probar todas las IP no sirve sin conocer el secreto.
		assertThat(new HuellaDeClaves("otro-secreto-distinto-9876543210").de("intentos", "ip:10.0.0.1"))
				.isNotEqualTo(huella.de("intentos", "ip:10.0.0.1"));
	}

	@Test
	void laLlaveSeDerivaDelSecretoYNoEsElSecretoConUnPrefijo() throws Exception {
		// La fórmula anterior (SCRUM-216) usaba "limites-de-uso:" + secreto como llave. Con ella, conocer la llave de las huellas era
		// conocer el secreto que firma los tokens. Ahora la llave sale de un HMAC del secreto (SCRUM-227): la huella ya no coincide.
		javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
		mac.init(new javax.crypto.spec.SecretKeySpec("limites-de-uso:secreto-de-prueba-0123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
		String conLaFormulaAnterior = java.util.HexFormat.of().formatHex(mac.doFinal("intentos\u0000ip:10.0.0.1".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		assertThat(huella.de("intentos", "ip:10.0.0.1")).isNotEqualTo(conLaFormulaAnterior);
	}

	@Test
	void elEspacioYLaClaveNoSePuedenConfundirAlUnirlos() {
		// «a» + «bc» no puede dar lo mismo que «ab» + «c».
		assertThat(huella.de("a", "bc")).isNotEqualTo(huella.de("ab", "c"));
	}
}
