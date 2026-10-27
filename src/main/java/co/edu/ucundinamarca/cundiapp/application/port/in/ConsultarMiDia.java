package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AgendaDelDia;
import java.time.LocalDate;
import java.util.Map;

/**
 * La pantalla del día del estudiante (RF07, SCRUM-31 y SCRUM-32): sus clases de un día, con la próxima
 * destacada, y el próximo día con clases cuando ese no tiene.
 *
 * <p>Sin fecha es hoy en Bogotá, y «ahora» es la hora de Bogotá: el servidor decide qué día es y en qué
 * clase va el estudiante, no el reloj del teléfono.
 */
public interface ConsultarMiDia {

	Resultado ejecutar(int idEstudiante, LocalDate fecha);

	/** `actividades` dice cuántas actividades de evaluación tiene cada asignatura, por matrícula. */
	record Resultado(LocalDate hoy, AgendaDelDia agenda, Map<Integer, Integer> actividades) {

		public Resultado {
			actividades = Map.copyOf(actividades);
		}

		/** Una actividad extra (SCRUM-266) no tiene matrícula ni actividades de evaluación: cero. */
		public int actividadesDe(Integer idMatricula) {
			return idMatricula == null ? 0 : actividades.getOrDefault(idMatricula, 0);
		}
	}
}
