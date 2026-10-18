package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciasDeAvisosRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Guarda las preferencias de avisos en la configuración del estudiante (RF10, SCRUM-38). La fila de
 * {@code configuracion_estudiante} la crea la base de datos al nacer el estudiante, así que aquí solo se
 * lee y se actualiza.
 */
@Component
class PreferenciasDeAvisosAdaptador implements PreferenciasDeAvisosRepositorio {

	private final JdbcTemplate jdbc;

	PreferenciasDeAvisosAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public PreferenciasDeAvisos de(int idEstudiante) {
		return jdbc.query("""
				SELECT notificaciones_activas, avisar_vencimientos, avisar_cambio_riesgo, anticipacion_aviso_horas
				  FROM configuracion_estudiante WHERE id_estudiante = ?""",
				(fila, numero) -> new PreferenciasDeAvisos(
						fila.getBoolean("notificaciones_activas"),
						fila.getBoolean("avisar_vencimientos"),
						fila.getBoolean("avisar_cambio_riesgo"),
						fila.getInt("anticipacion_aviso_horas")),
				idEstudiante).stream().findFirst().orElse(PreferenciasDeAvisos.PREDETERMINADAS);
	}

	@Override
	public void guardar(int idEstudiante, PreferenciasDeAvisos preferencias) {
		jdbc.update("""
				UPDATE configuracion_estudiante
				   SET notificaciones_activas = ?, avisar_vencimientos = ?, avisar_cambio_riesgo = ?,
				       anticipacion_aviso_horas = ?
				 WHERE id_estudiante = ?""",
				preferencias.activos(), preferencias.vencimientos(), preferencias.cambioDeRiesgo(),
				preferencias.anticipacionEnHoras(), idEstudiante);
	}
}
