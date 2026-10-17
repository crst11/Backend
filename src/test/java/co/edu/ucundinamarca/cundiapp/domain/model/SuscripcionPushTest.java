package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.ClavesDePushDePrueba;
import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * La suscripción Web Push (RF10, SCRUM-37). Lo más importante es lo que **no** se acepta: el servidor
 * llamará a esa dirección, así que solo valen las de los servicios de avisos conocidos.
 */
class SuscripcionPushTest {

	private static final ClavesDePushDePrueba.Suscripcion CLAVES = ClavesDePushDePrueba.nueva();
	private static final String FCM = "https://fcm.googleapis.com/fcm/send/abc123";

	private static SuscripcionPush con(String endpoint) {
		return new SuscripcionPush(endpoint, CLAVES.clavePublica(), CLAVES.secretoEnBase64());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			FCM,
			"https://updates.push.services.mozilla.com/wpush/v2/abc",
			"https://web.push.apple.com/QWxhZGRpbjpvcGVuIHNlc2FtZQ",
			"https://wns2-pn1p.notify.windows.com/w/?token=abc"
	})
	void aceptaLosServiciosDeAvisosConocidos(String endpoint) {
		assertThat(con(endpoint).endpoint()).isEqualTo(endpoint);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://fcm.googleapis.com/fcm/send/abc",
			"https://localhost/avisos",
			"https://127.0.0.1/avisos",
			"https://169.254.169.254/latest/meta-data",
			"https://backend:8080/api/mis/cuenta",
			"https://fcm.googleapis.com.atacante.com/fcm/send/abc",
			"https://atacante.com/fcm.googleapis.com",
			"https://usuario:clave@fcm.googleapis.com/fcm/send/abc",
			"https://fcm.googleapis.com:8443/fcm/send/abc",
			"ftp://fcm.googleapis.com/abc",
			"no es una dirección"
	})
	void rechazaCualquierOtraDireccion(String endpoint) {
		assertThatThrownBy(() -> con(endpoint)).isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void unaDireccionDeUnServicioDesconocidoDiceQueNoSeAdmite() {
		assertThatThrownBy(() -> con("https://servicio.ejemplo.com/avisos"))
				.hasMessageContaining("todavía no admitimos");
	}

	@Test
	void rechazaUnEndpointVacioOMuyLargo() {
		assertThatThrownBy(() -> con("")).isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> con("https://fcm.googleapis.com/" + "a".repeat(500)))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void laClavePublicaTieneQueSerUnPuntoSinComprimirDe65Bytes() {
		byte[] comprimida = new byte[33];
		comprimida[0] = 0x02;
		byte[] sinMarca = ClavesDePushDePrueba.sinComprimir((java.security.interfaces.ECPublicKey) CLAVES.navegador().getPublic());
		sinMarca[0] = 0x00;
		String sinMarcaEn64 = Base64.getUrlEncoder().withoutPadding().encodeToString(sinMarca);

		assertThatThrownBy(() -> new SuscripcionPush(FCM, Base64.getUrlEncoder().withoutPadding().encodeToString(comprimida), CLAVES.secretoEnBase64()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new SuscripcionPush(FCM, sinMarcaEn64, CLAVES.secretoEnBase64()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new SuscripcionPush(FCM, "%%%", CLAVES.secretoEnBase64()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new SuscripcionPush(FCM, null, CLAVES.secretoEnBase64()))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void elSecretoDeAutenticacionTieneQueTener16Bytes() {
		String corto = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[8]);

		assertThatThrownBy(() -> new SuscripcionPush(FCM, CLAVES.clavePublica(), corto))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
		assertThatThrownBy(() -> new SuscripcionPush(FCM, CLAVES.clavePublica(), ""))
				.isInstanceOf(ReglaDeNegocioVioladaException.class);
	}

	@Test
	void devuelveLasLlavesEnBytes() {
		SuscripcionPush suscripcion = con(FCM);

		assertThat(suscripcion.clavePublicaEnBytes()).hasSize(65).startsWith((byte) 0x04);
		assertThat(suscripcion.secretoEnBytes()).isEqualTo(CLAVES.secreto());
	}
}
