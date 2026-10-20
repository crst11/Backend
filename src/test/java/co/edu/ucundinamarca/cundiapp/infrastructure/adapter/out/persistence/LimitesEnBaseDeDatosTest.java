package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import co.edu.ucundinamarca.cundiapp.application.port.out.RelojPort;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Los limitadores contados en PostgreSQL real (12 factores, factor 6, SCRUM-216). Además del comportamiento de siempre
 * (los 5 fallos, los 20 por IP, los 15 minutos, la ventana fija), lo que esta prueba demuestra es lo que la memoria no
 * podía: que <b>dos instancias comparten la cuenta</b>, que <b>sobrevive a un reinicio</b>, que <b>hilos simultáneos no
 * pierden cuentas</b> y que <b>la tabla no guarda correos ni IP</b>.
 *
 * <p>No es transaccional a propósito: los hilos de la prueba concurrente cada uno usa su propia conexión y necesitan
 * ver lo que confirman los demás. Cada prueba usa claves únicas para no pisar a las otras.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LimitesEnBaseDeDatosTest {

	private static final Duration VENTANA = Duration.ofMinutes(10);

	private static final class RelojManual implements RelojPort {
		private Instant ahora = Instant.parse("2026-10-14T10:00:00Z");

		@Override
		public Instant ahora() {
			return ahora;
		}

		void avanzar(Duration tiempo) {
			ahora = ahora.plus(tiempo);
		}
	}

	@Autowired
	private JdbcTemplate jdbc;

	private final RelojManual reloj = new RelojManual();
	private final HuellaDeClaves huella = new HuellaDeClaves("secreto-de-prueba-0123456789");

	private LimitadorDeIntentosEnBaseDeDatos intentos() {
		return new LimitadorDeIntentosEnBaseDeDatos(jdbc, huella, reloj);
	}

	private LimitadorDePeticionesEnBaseDeDatos peticiones() {
		return new LimitadorDePeticionesEnBaseDeDatos(jdbc, huella, reloj);
	}

	private static String unica(String base) {
		return base + "." + UUID.randomUUID();
	}

	private int conteoGuardado(String espacio, String clave) {
		return jdbc.queryForObject("SELECT conteo FROM limite_de_uso WHERE clave = ?", Integer.class, huella.de(espacio, clave));
	}

	// --- Intentos fallidos -----------------------------------------------------------------------------------

	@Test
	void unCorreoSeBloqueaAlQuintoFalloSeguido() {
		String correo = "correo:" + unica("ana") + "@ucundinamarca.edu.co";
		for (int i = 0; i < 4; i++) {
			intentos().registrarFallo(correo);
		}
		assertThat(intentos().estaBloqueado(correo)).isFalse();

		intentos().registrarFallo(correo);

		assertThat(intentos().estaBloqueado(correo)).isTrue();
	}

	@Test
	void unaIpTolera20FallosPorqueVariosEstudiantesComparteRed() {
		String ip = "ip:" + unica("10.0.0.1");
		for (int i = 0; i < 19; i++) {
			intentos().registrarFallo(ip);
		}
		assertThat(intentos().estaBloqueado(ip)).isFalse();

		intentos().registrarFallo(ip);

		assertThat(intentos().estaBloqueado(ip)).isTrue();
	}

	@Test
	void elBloqueoSeLevantaA15MinutosDelUltimoFallo() {
		String correo = "correo:" + unica("luis");
		for (int i = 0; i < 5; i++) {
			intentos().registrarFallo(correo);
		}
		reloj.avanzar(Duration.ofMinutes(14));
		assertThat(intentos().estaBloqueado(correo)).isTrue();

		reloj.avanzar(Duration.ofMinutes(2));

		assertThat(intentos().estaBloqueado(correo)).isFalse();
	}

	@Test
	void cadaFalloReiniciaLosQuinceMinutosDelBloqueo() {
		String correo = "correo:" + unica("marta");
		for (int i = 0; i < 4; i++) {
			intentos().registrarFallo(correo);
			reloj.avanzar(Duration.ofMinutes(10));
		}
		// Cuatro fallos separados 10 minutos siguen siendo seguidos: cada uno estaba dentro de los 15 del anterior.
		intentos().registrarFallo(correo);

		assertThat(intentos().estaBloqueado(correo)).isTrue();
	}

	@Test
	void despuesDe15MinutosSinFallarLaCuentaVuelveACeroYNoSuma() {
		String correo = "correo:" + unica("sara");
		for (int i = 0; i < 4; i++) {
			intentos().registrarFallo(correo);
		}
		reloj.avanzar(Duration.ofMinutes(16));

		intentos().registrarFallo(correo);

		assertThat(conteoGuardado("intentos", correo)).isEqualTo(1);
		assertThat(intentos().estaBloqueado(correo)).isFalse();
	}

	@Test
	void unInicioDeSesionCorrectoReiniciaLaCuenta() {
		String correo = "correo:" + unica("pedro");
		for (int i = 0; i < 4; i++) {
			intentos().registrarFallo(correo);
		}

		intentos().reiniciar(correo);
		intentos().registrarFallo(correo);

		assertThat(intentos().estaBloqueado(correo)).isFalse();
		assertThat(conteoGuardado("intentos", correo)).isEqualTo(1);
	}

	@Test
	void cadaClaveCuentaPorSeparado() {
		String una = "correo:" + unica("uno");
		String otra = "correo:" + unica("otro");
		for (int i = 0; i < 5; i++) {
			intentos().registrarFallo(una);
		}

		assertThat(intentos().estaBloqueado(una)).isTrue();
		assertThat(intentos().estaBloqueado(otra)).isFalse();
	}

	@Test
	void dosInstanciasDelBackendComparteLaMismaCuenta() {
		String correo = "correo:" + unica("compartido");
		LimitadorDeIntentosEnBaseDeDatos instanciaA = intentos();
		LimitadorDeIntentosEnBaseDeDatos instanciaB = intentos();

		for (int i = 0; i < 3; i++) {
			instanciaA.registrarFallo(correo);
		}
		for (int i = 0; i < 2; i++) {
			instanciaB.registrarFallo(correo);
		}

		assertThat(instanciaA.estaBloqueado(correo)).isTrue();
		assertThat(instanciaB.estaBloqueado(correo)).isTrue();
	}

	@Test
	void laCuentaSobreviveAUnReinicioDeLaAplicacion() {
		String correo = "correo:" + unica("reinicio");
		for (int i = 0; i < 5; i++) {
			intentos().registrarFallo(correo);
		}

		// Un despliegue crea un limitador nuevo, sin nada en memoria: con el mapa de antes el bloqueo se perdía aquí.
		LimitadorDeIntentosEnBaseDeDatos despuesDelDespliegue = new LimitadorDeIntentosEnBaseDeDatos(jdbc, huella, reloj);

		assertThat(despuesDelDespliegue.estaBloqueado(correo)).isTrue();
	}

	/**
	 * SCRUM-236: dos peticiones casi simultaneas de la misma IP leen el reloj antes de turnarse para escribir, asi que la segunda puede
	 * llegar con una hora unas millonesimas ANTERIOR a la que ya quedo guardada. La restriccion {@code ck_limite_de_uso_orden}
	 * (ultima >= primera) la rechazaba y la peticion fallaba con un error que el cliente veia como un 401 sin cuerpo. Una IP compartida
	 * (todo un campus detras de una sola) las produce a diario.
	 */
	@Test
	void unaPeticionQueLlegaConUnaHoraUnPocoAnteriorNoRompeLaCuenta() {
		String ip = unica("ip");
		assertThat(peticiones().esperaSiSeExcede(ip, 100, VENTANA)).isEmpty();

		reloj.avanzar(Duration.ofNanos(-8_000));
		assertThat(peticiones().esperaSiSeExcede(ip, 100, VENTANA)).isEmpty();

		assertThat(conteoGuardado("peticiones", ip)).isEqualTo(2);
	}

	/** Lo mismo para los intentos fallidos de inicio de sesion: dos fallos casi simultaneos con las horas desordenadas (SCRUM-236). */
	@Test
	void unFalloQueLlegaConUnaHoraUnPocoAnteriorNoRompeLaCuenta() {
		String correo = unica("ana") + "@ucundinamarca.edu.co";
		intentos().registrarFallo(correo);

		reloj.avanzar(Duration.ofNanos(-8_000));
		intentos().registrarFallo(correo);

		assertThat(conteoGuardado("intentos", correo)).isEqualTo(2);
	}

	@Test
	void hilosAlMismoTiempoNoPierdenNingunaCuenta() throws Exception {
		String ip = "ip:" + unica("10.9.9.9");
		int hilos = 30;
		ExecutorService pool = Executors.newFixedThreadPool(hilos);
		try {
			List<Future<?>> trabajos = IntStream.range(0, hilos)
					.<Future<?>>mapToObj(i -> pool.submit(() -> intentos().registrarFallo(ip)))
					.toList();
			for (Future<?> trabajo : trabajos) {
				trabajo.get();
			}
		} finally {
			pool.shutdownNow();
		}

		assertThat(conteoGuardado("intentos", ip)).isEqualTo(hilos);
	}

	// --- Peticiones por ventana ------------------------------------------------------------------------------

	@Test
	void dejaPasarHastaElMaximoYRechazaLaSiguiente() {
		String clave = unica("pdf");
		for (int i = 0; i < 3; i++) {
			assertThat(peticiones().esperaSiSeExcede(clave, 3, VENTANA)).isEmpty();
		}

		assertThat(peticiones().esperaSiSeExcede(clave, 3, VENTANA)).isPresent();
	}

	@Test
	void diceCuantoFaltaParaQueSeLibere() {
		String clave = unica("pdf");
		for (int i = 0; i < 3; i++) {
			peticiones().esperaSiSeExcede(clave, 3, VENTANA);
		}
		reloj.avanzar(Duration.ofMinutes(4));

		assertThat(peticiones().esperaSiSeExcede(clave, 3, VENTANA)).contains(Duration.ofMinutes(6));
	}

	@Test
	void pasadaLaVentanaElContadorVuelveACero() {
		String clave = unica("pdf");
		for (int i = 0; i < 4; i++) {
			peticiones().esperaSiSeExcede(clave, 3, VENTANA);
		}
		reloj.avanzar(VENTANA);

		assertThat(peticiones().esperaSiSeExcede(clave, 3, VENTANA)).isEmpty();
		assertThat(conteoGuardado("peticiones", clave)).isEqualTo(1);
	}

	@Test
	void cadaClaveTieneSuPropiaVentana() {
		String una = unica("estudiante-uno");
		String otra = unica("estudiante-dos");
		for (int i = 0; i < 4; i++) {
			peticiones().esperaSiSeExcede(una, 3, VENTANA);
		}

		assertThat(peticiones().esperaSiSeExcede(una, 3, VENTANA)).isPresent();
		assertThat(peticiones().esperaSiSeExcede(otra, 3, VENTANA)).isEmpty();
	}

	@Test
	void dosInstanciasDelBackendComparteElCupo() {
		String clave = unica("compartido");
		LimitadorDePeticionesEnBaseDeDatos instanciaA = peticiones();
		LimitadorDePeticionesEnBaseDeDatos instanciaB = peticiones();

		instanciaA.esperaSiSeExcede(clave, 3, VENTANA);
		instanciaB.esperaSiSeExcede(clave, 3, VENTANA);
		instanciaA.esperaSiSeExcede(clave, 3, VENTANA);

		// Con un mapa por instancia, cada una habría contado dos como máximo y la cuarta pasaría.
		assertThat(instanciaB.esperaSiSeExcede(clave, 3, VENTANA)).isPresent();
	}

	@Test
	void hilosAlMismoTiempoSeRepartenExactamenteElCupo() throws Exception {
		String clave = unica("rafaga");
		int hilos = 40;
		int cupo = 25;
		AtomicInteger permitidas = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(hilos);
		try {
			List<Future<?>> trabajos = IntStream.range(0, hilos)
					.<Future<?>>mapToObj(i -> pool.submit(() -> {
						if (peticiones().esperaSiSeExcede(clave, cupo, VENTANA).isEmpty()) {
							permitidas.incrementAndGet();
						}
					}))
					.toList();
			for (Future<?> trabajo : trabajos) {
				trabajo.get();
			}
		} finally {
			pool.shutdownNow();
		}

		assertThat(permitidas.get()).isEqualTo(cupo);
	}

	// --- Lo que no puede haber en la tabla -------------------------------------------------------------------

	@Test
	void unaMismaClaveEnLosDosLimitesNoSeMezcla() {
		String clave = unica("misma");
		intentos().registrarFallo(clave);
		peticiones().esperaSiSeExcede(clave, 3, VENTANA);

		assertThat(conteoGuardado("intentos", clave)).isEqualTo(1);
		assertThat(conteoGuardado("peticiones", clave)).isEqualTo(1);
	}

	@Test
	void laTablaNoGuardaNiElCorreoNiLaIpEnClaro() {
		String correo = "correo:persona.privada." + UUID.randomUUID() + "@ucundinamarca.edu.co";
		intentos().registrarFallo(correo);
		intentos().registrarFallo("ip:203.0.113.77");
		peticiones().esperaSiSeExcede("correos-por-ip:203.0.113.77", 20, VENTANA);

		Integer conAlgoLegible = jdbc.queryForObject(
				"SELECT count(*) FROM limite_de_uso WHERE clave LIKE '%persona%' OR clave LIKE '%203.0.113%' OR clave LIKE '%ucundinamarca%'",
				Integer.class);
		Integer noSonHuellas = jdbc.queryForObject("SELECT count(*) FROM limite_de_uso WHERE clave !~ '^[0-9a-f]{64}$'", Integer.class);

		assertThat(conAlgoLegible).isZero();
		assertThat(noSonHuellas).isZero();
	}

	// --- Limpieza --------------------------------------------------------------------------------------------

	@Test
	void laLimpiezaBorraLoQueLlevaMasDeUnDiaSinTocarseYConservaLoReciente() {
		String vieja = "correo:" + unica("vieja");
		String reciente = "correo:" + unica("reciente");
		intentos().registrarFallo(vieja);
		reloj.avanzar(Duration.ofDays(2));
		intentos().registrarFallo(reciente);

		int borradas = new LimpiezaDeLimites(jdbc, reloj).limpiarLoAnteriorA(reloj.ahora().minus(LimpiezaDeLimites.CONSERVAR));

		assertThat(borradas).isGreaterThanOrEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM limite_de_uso WHERE clave = ?", Integer.class, huella.de("intentos", vieja))).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM limite_de_uso WHERE clave = ?", Integer.class, huella.de("intentos", reciente))).isEqualTo(1);
	}
}
