package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * De dónde sale la IP con la que se limitan los intentos (SCRUM-71).
 *
 * <p>Detrás de CloudFront y el balanceador, getRemoteAddr() es la IP del balanceador: la misma para
 * todos los estudiantes. Los límites por IP compartirían un solo cupo, y quien lo agotara dejaría
 * fuera a todos. Se midió con la imagen real detrás de nginx: el backend registraba 172.20.0.4, la
 * del contenedor nginx, y no la del cliente.
 *
 * <p>Pero X-Forwarded-For no se puede leer a ciegas: lo escribe también el cliente. Cada proxy de
 * confianza agrega al final la IP de quien le habló, así que solo se cree lo que ellos agregaron.
 */
class IpDelClienteTest {

	private static final String CLIENTE = "190.24.10.7";
	private static final String BALANCEADOR = "10.0.3.25";
	private static final String BORDE_DE_CLOUDFRONT = "130.176.4.9";

	private static MockHttpServletRequest peticion(String remota, String forwardedFor) {
		var peticion = new MockHttpServletRequest();
		peticion.setRemoteAddr(remota);
		if (forwardedFor != null) {
			peticion.addHeader("X-Forwarded-For", forwardedFor);
		}
		return peticion;
	}

	@Nested
	@DisplayName("Sin proxy de confianza (local y pruebas)")
	class SinProxy {

		private final IpDelCliente ip = new IpDelCliente(0);

		@Test
		void usaLaIpDeLaConexion() {
			assertThat(ip.de(peticion(CLIENTE, null))).isEqualTo(CLIENTE);
		}

		@Test
		void ignoraXForwardedForPorqueLoEscribeElCliente() {
			// Sin esto, cualquiera se saltaría el límite poniéndose una IP distinta en cada petición.
			assertThat(ip.de(peticion(CLIENTE, "1.2.3.4"))).isEqualTo(CLIENTE);
		}
	}

	@Nested
	@DisplayName("Detrás del balanceador (un proxy)")
	class UnProxy {

		private final IpDelCliente ip = new IpDelCliente(1);

		@Test
		void tomaLaIpQueElBalanceadorAgrego() {
			assertThat(ip.de(peticion(BALANCEADOR, CLIENTE))).isEqualTo(CLIENTE);
		}

		@Test
		void unaIpFalsaPuestaPorElClienteNoGana() {
			// El cliente mandó "9.9.9.9"; el balanceador le agregó la IP real al final.
			assertThat(ip.de(peticion(BALANCEADOR, "9.9.9.9, " + CLIENTE))).isEqualTo(CLIENTE);
		}
	}

	@Nested
	@DisplayName("Detrás de CloudFront y el balanceador (dos proxies)")
	class DosProxies {

		private final IpDelCliente ip = new IpDelCliente(2);

		@Test
		void tomaLaIpQueAgregoCloudFront() {
			assertThat(ip.de(peticion(BALANCEADOR, CLIENTE + ", " + BORDE_DE_CLOUDFRONT))).isEqualTo(CLIENTE);
		}

		@Test
		void unaIpFalsaPuestaPorElClienteNoGana() {
			assertThat(ip.de(peticion(BALANCEADOR, "9.9.9.9, " + CLIENTE + ", " + BORDE_DE_CLOUDFRONT)))
					.isEqualTo(CLIENTE);
		}

		@Test
		void aceptaIpv6() {
			assertThat(ip.de(peticion(BALANCEADOR, "2800:484:1c80::1, 2600:9000::1"))).isEqualTo("2800:484:1c80::1");
		}

		@Test
		void ignoraLosEspaciosAlrededorDeCadaIp() {
			assertThat(ip.de(peticion(BALANCEADOR, "  " + CLIENTE + " ,   " + BORDE_DE_CLOUDFRONT + "  ")))
					.isEqualTo(CLIENTE);
		}
	}

	@Nested
	@DisplayName("Cuando el encabezado no es de fiar")
	class EncabezadoDudoso {

		private final IpDelCliente ip = new IpDelCliente(2);

		@Test
		void sinEncabezadoUsaLaIpDeLaConexion() {
			// Pasa en las sondas del propio balanceador: llegan sin pasar por CloudFront.
			assertThat(ip.de(peticion(BALANCEADOR, null))).isEqualTo(BALANCEADOR);
		}

		@Test
		void conMenosEntradasQueProxiesNoSeInventaUnCliente() {
			// Se esperaban dos entradas y hay una: alguien se saltó un proxy. Mejor la IP de la conexión.
			assertThat(ip.de(peticion(BALANCEADOR, CLIENTE))).isEqualTo(BALANCEADOR);
		}

		@Test
		void unTextoQueNoEsUnaIpSeDescarta() {
			// Si se aceptara, cualquiera crearía un cupo nuevo por petición con un texto distinto cada vez.
			assertThat(ip.de(peticion(BALANCEADOR, "no-soy-una-ip, " + BORDE_DE_CLOUDFRONT))).isEqualTo(BALANCEADOR);
		}

		@Test
		void unEncabezadoVacioSeTrataComoSiNoEstuviera() {
			assertThat(ip.de(peticion(BALANCEADOR, ""))).isEqualTo(BALANCEADOR);
		}

		@Test
		void unTextoLargoSeDescarta() {
			assertThat(ip.de(peticion(BALANCEADOR, "1".repeat(200) + ", " + BORDE_DE_CLOUDFRONT)))
					.isEqualTo(BALANCEADOR);
		}
	}

	@Test
	void unaConfiguracionNegativaNoSePermite() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> new IpDelCliente(-1))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
