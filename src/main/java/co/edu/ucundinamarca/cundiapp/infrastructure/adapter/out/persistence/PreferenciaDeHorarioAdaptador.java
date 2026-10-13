package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.out.persistence;

import co.edu.ucundinamarca.cundiapp.application.port.out.PreferenciaDeHorarioRepositorio;
import co.edu.ucundinamarca.cundiapp.domain.model.OrdenDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciaDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.VistaDelHorario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Guarda la preferencia de horario en la configuración del estudiante (RF04, SCRUM-26).
 *
 * <p>La fila de {@code configuracion_estudiante} la crea la base de datos al nacer el estudiante, así
 * que aquí solo se lee y se actualiza. Con JdbcTemplate: son tres columnas de una fila.
 */
@Component
class PreferenciaDeHorarioAdaptador implements PreferenciaDeHorarioRepositorio {

	private final JdbcTemplate jdbc;

	PreferenciaDeHorarioAdaptador(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public PreferenciaDeHorario de(int idEstudiante) {
		return jdbc.query("""
				SELECT horario_vista, horario_sede, horario_orden
				  FROM configuracion_estudiante WHERE id_estudiante = ?""",
				(fila, numero) -> new PreferenciaDeHorario(
						VistaDelHorario.desdeBd(fila.getString("horario_vista")),
						fila.getString("horario_sede"),
						OrdenDelHorario.desdeBd(fila.getString("horario_orden"))),
				idEstudiante).stream().findFirst().orElse(PreferenciaDeHorario.PREDETERMINADA);
	}

	@Override
	public void guardar(int idEstudiante, PreferenciaDeHorario preferencia) {
		jdbc.update("""
				UPDATE configuracion_estudiante
				   SET horario_vista = ?, horario_sede = ?, horario_orden = ?
				 WHERE id_estudiante = ?""",
				preferencia.vista().valorEnBd(), preferencia.sede(), preferencia.orden().valorEnBd(), idEstudiante);
	}
}
