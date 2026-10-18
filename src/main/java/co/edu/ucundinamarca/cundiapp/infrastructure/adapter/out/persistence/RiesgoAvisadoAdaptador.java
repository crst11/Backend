package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.RiesgoAvisadoRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Guarda en {@code riesgo_avisado} el último nivel de riesgo conocido de cada matrícula (RF10, SCRUM-50).
 * Con JdbcTemplate: es una sola fila por matrícula, leída y escrita por su clave.
 */
@Component
class RiesgoAvisadoAdaptador implements RiesgoAvisadoRepositorio {

	private final JdbcTemplate jdbc;

	RiesgoAvisadoAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<NivelDeRiesgo> nivelDe(int idMatricula) {
		return jdbc.query("SELECT nivel FROM riesgo_avisado WHERE id_matricula = ?",
				(fila, numero) -> NivelDeRiesgo.valueOf(fila.getString("nivel").toUpperCase(Locale.ROOT)),
				idMatricula).stream().findFirst();
	}

	@Override
	public void guardar(int idMatricula, NivelDeRiesgo nivel, Instant ahora) {
		jdbc.update("""
				INSERT INTO riesgo_avisado (id_matricula, nivel, fecha_actualizacion) VALUES (?, ?, ?)
				ON CONFLICT (id_matricula) DO UPDATE SET nivel = EXCLUDED.nivel, fecha_actualizacion = EXCLUDED.fecha_actualizacion""",
				idMatricula, nivel.name().toLowerCase(Locale.ROOT), OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC));
	}
}
