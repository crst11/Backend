package co.edu.ucundinamarca.cundiapp.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enciende las tareas programadas, hoy solo la ronda de avisos (RF10). Se apaga junto con ella. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "cundiapp.avisos.programados", havingValue = "true", matchIfMissing = true)
class ProgramacionConfig {
}
