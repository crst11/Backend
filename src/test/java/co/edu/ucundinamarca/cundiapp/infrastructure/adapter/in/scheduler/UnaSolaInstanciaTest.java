package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.ucundinamarca.cundiapp.TestcontainersConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * La exclusión entre instancias contra PostgreSQL real (12 factores, factor 9, SCRUM-220). Cada hilo usa su propia
 * conexión, igual que dos instancias del backend: lo que se demuestra es que <b>mientras una corre la ronda la otra
 * se la salta</b>, que <b>el candado se suelta aunque la tarea falle</b> y que <b>llaves distintas no se estorban</b>.
 *
 * <p>Las llaves son propias de la prueba para no chocar con la de la ronda real.
 */
@SpringBootTest(properties = "cundiapp.avisos.programados=false")
@Import(TestcontainersConfiguration.class)
class UnaSolaInstanciaTest {

	private static final long LLAVE = 920_001L;
	private static final long OTRA_LLAVE = 920_002L;

	/** El pool de la aplicación: el que atiende las peticiones web. El candado NO debe gastarle conexiones (SCRUM-228). */
	@Autowired
	private DataSource datosDeLaAplicacion;

	@Autowired
	private JdbcConnectionDetails conexion;

	private final List<UnaSolaInstancia> creadas = new CopyOnWriteArrayList<>();

	private UnaSolaInstancia instancia() {
		UnaSolaInstancia nueva = new UnaSolaInstancia(conexion);
		creadas.add(nueva);
		return nueva;
	}

	@AfterEach
	void cerrarLasInstancias() {
		creadas.forEach(UnaSolaInstancia::cerrar);
		creadas.clear();
	}

	@Test
	void elCandadoNoGastaConexionesDelPoolDeLaAplicacion() {
		// Hikari del pool de la aplicación (en producción son 5 conexiones para todo): una ronda lenta de avisos con el candado
		// agarrado no puede dejar a las peticiones web sin conexión. El candado usa una conexión propia, aparte.
		var pool = ((HikariDataSource) datosDeLaAplicacion).getHikariPoolMXBean();
		int activasAntes = pool.getActiveConnections();
		AtomicInteger activasConElCandado = new AtomicInteger(-1);

		boolean corrio = instancia().ejecutar(LLAVE, () -> activasConElCandado.set(pool.getActiveConnections()));

		assertThat(corrio).isTrue();
		assertThat(activasConElCandado).hasValue(activasAntes);

		// Control: la métrica sí se mueve cuando algo usa el pool de la aplicación. Sin esto la prueba pasaría igual si el contador
		// no midiera nada.
		try (var ocupada = datosDeLaAplicacion.getConnection()) {
			assertThat(ocupada.isValid(2)).isTrue();
			assertThat(pool.getActiveConnections()).isEqualTo(activasAntes + 1);
		} catch (java.sql.SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	void sinCompetenciaLaTareaCorre() {
		AtomicInteger corridas = new AtomicInteger();

		boolean corrio = instancia().ejecutar(LLAVE, corridas::incrementAndGet);

		assertThat(corrio).isTrue();
		assertThat(corridas).hasValue(1);
	}

	@Test
	void mientrasUnaInstanciaCorreLaOtraSeSaltaLaRonda() throws Exception {
		ExecutorService hilos = Executors.newFixedThreadPool(2);
		try {
			CountDownLatch adentro = new CountDownLatch(1);
			CountDownLatch terminar = new CountDownLatch(1);
			Future<Boolean> primera = hilos.submit(() -> instancia().ejecutar(LLAVE, () -> {
				adentro.countDown();
				esperar(terminar);
			}));
			assertThat(adentro.await(10, TimeUnit.SECONDS)).isTrue();

			AtomicInteger corridasDeLaSegunda = new AtomicInteger();
			boolean corrioLaSegunda = hilos.submit(() -> instancia().ejecutar(LLAVE, corridasDeLaSegunda::incrementAndGet)).get(10, TimeUnit.SECONDS);

			assertThat(corrioLaSegunda).isFalse();
			assertThat(corridasDeLaSegunda).hasValue(0);

			terminar.countDown();
			assertThat(primera.get(10, TimeUnit.SECONDS)).isTrue();
		} finally {
			hilos.shutdownNow();
		}
	}

	@Test
	void alTerminarLaRondaLaSiguienteInstanciaPuedeCorrer() {
		assertThat(instancia().ejecutar(LLAVE, () -> { })).isTrue();
		assertThat(instancia().ejecutar(LLAVE, () -> { })).isTrue();
	}

	@Test
	void siLaTareaFallaElCandadoSeSueltaYLaExcepcionSale() {
		assertThatThrownBy(() -> instancia().ejecutar(LLAVE, () -> {
			throw new IllegalStateException("fallo de la ronda");
		})).isInstanceOf(IllegalStateException.class).hasMessage("fallo de la ronda");

		assertThat(instancia().ejecutar(LLAVE, () -> { })).isTrue();
	}

	@Test
	void llavesDistintasNoSeEstorban() throws Exception {
		ExecutorService hilos = Executors.newFixedThreadPool(2);
		try {
			CountDownLatch adentro = new CountDownLatch(1);
			CountDownLatch terminar = new CountDownLatch(1);
			Future<Boolean> primera = hilos.submit(() -> instancia().ejecutar(LLAVE, () -> {
				adentro.countDown();
				esperar(terminar);
			}));
			assertThat(adentro.await(10, TimeUnit.SECONDS)).isTrue();

			boolean corrioLaOtra = hilos.submit(() -> instancia().ejecutar(OTRA_LLAVE, () -> { })).get(10, TimeUnit.SECONDS);

			assertThat(corrioLaOtra).isTrue();
			terminar.countDown();
			assertThat(primera.get(10, TimeUnit.SECONDS)).isTrue();
		} finally {
			hilos.shutdownNow();
		}
	}

	private static void esperar(CountDownLatch seguro) {
		try {
			if (!seguro.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("la prueba no liberó la tarea a tiempo");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
