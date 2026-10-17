package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.ConfiguracionRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.Meta;
import co.edu.ucundinamarca.cundiapp.domain.model.UmbralesDeRiesgo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Guarda la meta de calificación en la configuración del estudiante (RF08, SCRUM-34).
 *
 * <p>La fila de {@code configuracion_estudiante} la crea la base de datos al nacer el estudiante, así
 * que aquí solo se lee y se actualiza. Con JdbcTemplate: es una columna de una fila.
 */
@Component
class ConfiguracionAdaptador implements ConfiguracionRepositorio {

	private final JdbcTemplate jdbc;

	ConfiguracionAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Meta metaDe(int idEstudiante) {
		return jdbc.query("SELECT meta_calificacion FROM configuracion_estudiante WHERE id_estudiante = ?",
				(fila, numero) -> new Meta(fila.getBigDecimal("meta_calificacion")),
				idEstudiante).stream().findFirst().orElse(Meta.PREDETERMINADA);
	}

	@Override
	public UmbralesDeRiesgo umbralesDe(int idEstudiante) {
		return jdbc.query("""
				SELECT umbral_riesgo_medio, umbral_riesgo_alto
				  FROM configuracion_estudiante WHERE id_estudiante = ?""",
				(fila, numero) -> new UmbralesDeRiesgo(
						fila.getBigDecimal("umbral_riesgo_medio"), fila.getBigDecimal("umbral_riesgo_alto")),
				idEstudiante).stream().findFirst().orElse(UmbralesDeRiesgo.PREDETERMINADOS);
	}

	@Override
	public void guardarUmbrales(int idEstudiante, UmbralesDeRiesgo umbrales) {
		jdbc.update("""
				UPDATE configuracion_estudiante
				   SET umbral_riesgo_medio = ?, umbral_riesgo_alto = ?
				 WHERE id_estudiante = ?""",
				umbrales.medio(), umbrales.alto(), idEstudiante);
	}

	@Override
	public void guardarMeta(int idEstudiante, Meta meta) {
		jdbc.update("UPDATE configuracion_estudiante SET meta_calificacion = ? WHERE id_estudiante = ?",
				meta.valor(), idEstudiante);
	}
}
