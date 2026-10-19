package co.edu.ucundinamarca.cundiapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * El apagado ordenado de Spring espera hasta {@code spring.lifecycle.timeout-per-shutdown-phase} a que terminen las
 * peticiones en curso, pero Docker mata el contenedor a los 10 segundos si el compose no dice otra cosa. Esa
 * diferencia no se ve en ninguna prueba ni en ningún log, y hace que cada despliegue corte lo que estuviera a medias
 * (12 factores, factor 9, SCRUM-212). Esta prueba existe para que nadie suba el tiempo de Spring o quite el de
 * Docker sin darse cuenta.
 */
class ApagadoOrdenadoTest {

	private static final Pattern SEGUNDOS = Pattern.compile("(\\d+)s");

	private static Duration segundosDe(String linea) {
		Matcher encontrado = SEGUNDOS.matcher(linea);
		assertThat(encontrado.find()).as("una duración en segundos en: " + linea).isTrue();
		return Duration.ofSeconds(Long.parseLong(encontrado.group(1)));
	}

	private static String lineaQueEmpiezaCon(String archivo, String prefijo) throws IOException {
		return Files.readAllLines(Path.of(archivo)).stream()
				.map(String::strip)
				.filter(linea -> linea.startsWith(prefijo))
				.findFirst()
				.orElseThrow(() -> new AssertionError(archivo + " no tiene «" + prefijo + "»"));
	}

	private static Duration esperaDeSpring() throws IOException {
		return segundosDe(lineaQueEmpiezaCon("src/main/resources/application.properties", "spring.lifecycle.timeout-per-shutdown-phase"));
	}

	@Test
	void elComposeDeLaInstanciaLeDaADockerMasTiempoQueElApagadoOrdenadoDeSpring() throws IOException {
		Duration docker = segundosDe(lineaQueEmpiezaCon("infra/aws/instancia/compose-backend.yml", "stop_grace_period:"));

		assertThat(docker).isGreaterThan(esperaDeSpring());
	}

	@Test
	void elComposeLocalLeDaADockerMasTiempoQueElApagadoOrdenadoDeSpring() throws IOException {
		Duration docker = segundosDe(lineaQueEmpiezaCon("docker-compose.yml", "stop_grace_period:"));

		assertThat(docker).isGreaterThan(esperaDeSpring());
	}

	@Test
	void springEspera25SegundosPorLasPeticionesEnCurso() throws IOException {
		assertThat(esperaDeSpring()).isEqualTo(Duration.ofSeconds(25));
		assertThat(lineaQueEmpiezaCon("src/main/resources/application.properties", "server.shutdown")).endsWith("graceful");
	}
}
