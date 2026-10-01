package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Hilos para el envío del código de verificación fuera de la petición (SCRUM-67).
 *
 * <p>Es un pool propio y acotado: si el servidor de correo se pone lento, la cola se llena y lo que
 * se frena es el envío de correos, no la atención de las peticiones. Al llenarse la cola el envío se
 * descarta y queda en el log; la persona siempre puede pedir otro código.
 */
@Configuration
@EnableAsync
class EnvioEnSegundoPlanoConfig {

	private static final Logger log = LoggerFactory.getLogger(EnvioEnSegundoPlanoConfig.class);

	@Bean("ejecutorDeEnvioDeCodigo")
	Executor ejecutorDeEnvioDeCodigo() {
		var ejecutor = new ThreadPoolTaskExecutor();
		ejecutor.setCorePoolSize(2);
		ejecutor.setMaxPoolSize(4);
		ejecutor.setQueueCapacity(100);
		ejecutor.setThreadNamePrefix("envio-codigo-");
		// Con la cola llena se descarta el envío en vez de romper el registro: la cuenta ya quedó
		// creada y la persona puede pedir otro código. Sin esto la petición terminaría en un 500.
		ejecutor.setRejectedExecutionHandler((tarea, pool) ->
				log.error("Cola de envío de códigos llena: se descartó un correo. Hay que pedir el código de nuevo"));
		// Al apagar, da tiempo a que salgan los correos que ya se estaban enviando.
		ejecutor.setWaitForTasksToCompleteOnShutdown(true);
		ejecutor.setAwaitTerminationSeconds(10);
		ejecutor.initialize();
		return ejecutor;
	}
}
