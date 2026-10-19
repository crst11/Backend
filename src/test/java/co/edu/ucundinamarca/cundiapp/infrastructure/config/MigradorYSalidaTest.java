package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * El modo de una sola ejecución (SCRUM-217, 12 factores, factor 12): la imagen migra el esquema y se apaga con un
 * código de salida que el script de despliegue lee. Aquí se prueba la decisión; el recorrido completo con la imagen real
 * contra PostgreSQL está en docs/despliegue.md.
 */
class MigradorYSalidaTest {

	private final List<Integer> salidas = new ArrayList<>();

	private MigradorYSalida migrador(MigrationInfo actual, int pendientes) {
		Flyway flyway = mock(Flyway.class);
		MigrationInfoService info = mock(MigrationInfoService.class);
		when(flyway.info()).thenReturn(info);
		when(info.current()).thenReturn(actual);
		when(info.pending()).thenReturn(new MigrationInfo[pendientes]);
		return new MigradorYSalida(flyway, mock(ConfigurableApplicationContext.class), salidas::add);
	}

	private static MigrationInfo enVersion(String version) {
		MigrationInfo info = mock(MigrationInfo.class);
		when(info.getVersion()).thenReturn(MigrationVersion.fromVersion(version));
		return info;
	}

	@Test
	void sale_con_cero_cuando_el_esquema_quedo_al_dia() throws Exception {
		migrador(enVersion("15"), 0).run(new DefaultApplicationArguments());

		assertThat(salidas).containsExactly(0);
	}

	@Test
	void sale_con_error_si_quedaron_migraciones_pendientes() throws Exception {
		// Pasaria si alguien arranca el migrador con spring.flyway.enabled=false: sin esto saldria "bien" sin migrar nada.
		migrador(enVersion("14"), 1).run(new DefaultApplicationArguments());

		assertThat(salidas).containsExactly(1);
	}

	@Test
	void sale_con_error_si_la_base_no_tiene_ninguna_migracion_aplicada() throws Exception {
		migrador(null, 0).run(new DefaultApplicationArguments());

		assertThat(salidas).containsExactly(1);
	}
}
