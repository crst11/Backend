package co.edu.ucundinamarca.cundiapp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Agrupar las sesiones vigentes por dispositivo (RF01, SCRUM-73).
 *
 * <p>Cada renovación del token rota el refresco y abre otra sesión, así que un solo navegador deja
 * una fila nueva cada 20 minutos. En pantalla eso se lee como si se hubieran metido a la cuenta
 * veinte veces. La rotación se queda —es lo que permite detectar un token robado—, pero el
 * estudiante ve un dispositivo por cadena.
 */
class DispositivoConSesionTest {

	private static final Instant AYER = Instant.parse("2026-10-03T20:00:00Z");
	private static final String CHROME = "Mozilla/5.0 (Windows NT 10.0) Chrome/141";
	private static final String ANDROID = "Mozilla/5.0 (Linux; Android 14) Chrome/141";

	private static Sesion sesion(int consecutivo, String userAgent, String ip, Duration desdeAyer) {
		Instant inicio = AYER.plus(desdeAyer);
		return new Sesion(1, consecutivo, MetodoDeAcceso.LOCAL, "huella" + consecutivo, inicio,
				inicio.plus(Duration.ofDays(7)), null, null, userAgent, ip);
	}

	@Test
	void dosSesionesDelMismoNavegadorSonUnSoloDispositivo() {
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(2, CHROME, "190.1.1.1", Duration.ofMinutes(20)),
						sesion(1, CHROME, "190.1.1.1", Duration.ZERO)),
				null);

		assertThat(dispositivos).hasSize(1);
		assertThat(dispositivos.getFirst().sesiones()).containsExactly(1, 2);
	}

	@Test
	void navegadoresDistintosSonDispositivosDistintos() {
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(2, ANDROID, "10.0.0.5", Duration.ofMinutes(30)),
						sesion(1, CHROME, "190.1.1.1", Duration.ZERO)),
				null);

		assertThat(dispositivos).hasSize(2);
		assertThat(dispositivos).extracting(DispositivoConSesion::dispositivo)
				.containsExactlyInAnyOrder(CHROME, ANDROID);
	}

	@Test
	void laCadenaGuardaDesdeCuandoEntroYCuandoFueLaUltimaVez() {
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(3, CHROME, "190.1.1.1", Duration.ofHours(2)),
						sesion(2, CHROME, "190.1.1.1", Duration.ofMinutes(20)),
						sesion(1, CHROME, "190.1.1.1", Duration.ZERO)),
				null);

		var chrome = dispositivos.getFirst();
		assertThat(chrome.primerAcceso()).isEqualTo(AYER);
		assertThat(chrome.ultimoAcceso()).isEqualTo(AYER.plus(Duration.ofHours(2)));
	}

	@Test
	void laIpEsLaDelAccesoMasReciente() {
		// Cambiar de wifi a datos no es otro dispositivo: es el mismo, desde otra red.
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(2, ANDROID, "10.0.0.5", Duration.ofHours(1)),
						sesion(1, ANDROID, "190.1.1.1", Duration.ZERO)),
				null);

		assertThat(dispositivos).hasSize(1);
		assertThat(dispositivos.getFirst().ip()).isEqualTo("10.0.0.5");
	}

	@Test
	void marcaElDispositivoDesdeElQueSeEstaMirando() {
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(3, ANDROID, "10.0.0.5", Duration.ofHours(3)),
						sesion(2, CHROME, "190.1.1.1", Duration.ofMinutes(20)),
						sesion(1, CHROME, "190.1.1.1", Duration.ZERO)),
				2);

		assertThat(dispositivos).filteredOn(DispositivoConSesion::esLaActual)
				.extracting(DispositivoConSesion::dispositivo)
				.containsExactly(CHROME);
	}

	@Test
	void elDispositivoActualVaDePrimero() {
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(3, ANDROID, "10.0.0.5", Duration.ofHours(3)),
						sesion(1, CHROME, "190.1.1.1", Duration.ZERO)),
				1);

		assertThat(dispositivos.getFirst().dispositivo()).isEqualTo(CHROME);
		assertThat(dispositivos.getFirst().esLaActual()).isTrue();
	}

	@Test
	void losDemasVanDelMasRecienteAlMasAntiguo() {
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(3, ANDROID, "10.0.0.5", Duration.ofHours(3)),
						sesion(2, "Firefox/130", "190.1.1.9", Duration.ofHours(5)),
						sesion(1, CHROME, "190.1.1.1", Duration.ZERO)),
				null);

		assertThat(dispositivos).extracting(DispositivoConSesion::dispositivo)
				.containsExactly("Firefox/130", ANDROID, CHROME);
	}

	@Test
	void sinSesionActualNoSeMarcaNinguna() {
		// Pasa con los tokens emitidos antes de que el JWT llevara el identificador de sesión.
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(1, CHROME, "190.1.1.1", Duration.ZERO)), null);

		assertThat(dispositivos).noneMatch(DispositivoConSesion::esLaActual);
	}

	@Test
	void unaSesionActualQueYaNoEstaVigenteTampocoMarcaNada() {
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(1, CHROME, "190.1.1.1", Duration.ZERO)), 99);

		assertThat(dispositivos).noneMatch(DispositivoConSesion::esLaActual);
	}

	@Test
	void lasQueNoDicenDesdeDondeQuedanJuntas() {
		// user_agent es opcional en el esquema; sin él no hay forma de distinguirlas.
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(2, null, "190.1.1.1", Duration.ofHours(1)),
						sesion(1, null, "190.1.1.1", Duration.ZERO)),
				null);

		assertThat(dispositivos).hasSize(1);
		assertThat(dispositivos.getFirst().dispositivo()).isNull();
		assertThat(dispositivos.getFirst().sesiones()).containsExactly(1, 2);
	}

	@Test
	void sinSesionesNoHayDispositivos() {
		assertThat(DispositivoConSesion.agrupar(List.of(), null)).isEmpty();
	}

	@Test
	void elDispositivoSeIdentificaPorSuSesionMasReciente() {
		// Es el consecutivo con el que la pantalla pide cerrarlo.
		var dispositivos = DispositivoConSesion.agrupar(
				List.of(sesion(7, CHROME, "190.1.1.1", Duration.ofHours(1)),
						sesion(4, CHROME, "190.1.1.1", Duration.ZERO)),
				null);

		assertThat(dispositivos.getFirst().consecutivo()).isEqualTo(7);
	}

	@Test
	void elMetodoEsConElQueEntroLaUltimaVez() {
		var conGoogle = new Sesion(1, 2, MetodoDeAcceso.GOOGLE, "huella2", AYER.plus(Duration.ofHours(1)),
				AYER.plus(Duration.ofDays(7)), null, null, CHROME, "190.1.1.1");

		var dispositivos = DispositivoConSesion.agrupar(
				List.of(conGoogle, sesion(1, CHROME, "190.1.1.1", Duration.ZERO)), null);

		assertThat(dispositivos.getFirst().metodo()).isEqualTo(MetodoDeAcceso.GOOGLE);
	}
}
