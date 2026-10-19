package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import java.util.function.IntConsumer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Modo de una sola ejecución: migra el esquema y se apaga (12 factores, factor 12, SCRUM-217). Es la misma imagen del
 * backend arrancada con {@code CUNDIAPP_MIGRAR_Y_SALIR=true}; Flyway ya migró cuando el contexto terminó de levantar, así
 * que aquí solo se comprueba que el esquema quedó al día y se sale con un código que el script de despliegue lee.
 *
 * <p>Existe para que **una migración que falla no tumbe lo que está sirviendo**. Antes, Flyway corría dentro del arranque
 * del backend nuevo: si fallaba, el contenedor entraba en un bucle de reinicios y solo después el script volvía a la imagen
 * anterior. Ahora el despliegue corre este paso primero; si sale con error, ni siquiera se toca el contenedor vigente.
 *
 * <p>Si Flyway falla, el contexto no llega a levantar y Spring Boot sale con código 1. Si llega y aun así quedan migraciones
 * pendientes (alguien lo arrancó con {@code spring.flyway.enabled=false}), sale con 1 también: que no diga "bien" sin haber
 * migrado nada.
 */
@Component
@ConditionalOnProperty(name = "cundiapp.migrar-y-salir", havingValue = "true")
class MigradorYSalida implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MigradorYSalida.class);

	private final Flyway flyway;
	private final ConfigurableApplicationContext contexto;
	private final IntConsumer terminar;

	@Autowired
	MigradorYSalida(Flyway flyway, ConfigurableApplicationContext contexto) {
		this(flyway, contexto, System::exit);
	}

	MigradorYSalida(Flyway flyway, ConfigurableApplicationContext contexto, IntConsumer terminar) {
		this.flyway = flyway;
		this.contexto = contexto;
		this.terminar = terminar;
	}

	@Override
	public void run(ApplicationArguments argumentos) {
		MigrationInfoService informacion = flyway.info();
		MigrationInfo actual = informacion.current();
		int pendientes = informacion.pending().length;
		if (actual == null || pendientes > 0) {
			log.error("El esquema no quedó al día: {} migraciones pendientes, versión actual {}",
					pendientes, actual == null ? "ninguna" : actual.getVersion());
			terminar.accept(1);
			return;
		}
		log.info("Migraciones al día: el esquema está en la versión {}", actual.getVersion());
		terminar.accept(SpringApplication.exit(contexto));
	}
}
