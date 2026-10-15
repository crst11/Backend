package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.DiaDelCalendario;
import java.time.LocalDate;
import java.util.List;

/**
 * Qué clase de día es cada fecha de un rango, y qué día es hoy en Bogotá (RF07, SCRUM-85).
 *
 * <p>«Hoy» lo dice el servidor, con la hora de Bogotá, y no el reloj del teléfono ni la zona del
 * servidor: así la pantalla del día cambia a la medianoche de Colombia para todos por igual.
 */
public interface ConsultarCalendario {

	/** Sin `desde` empieza hoy; sin `hasta` llega seis días después de `desde`. */
	Calendario ejecutar(LocalDate desde, LocalDate hasta);

	record Calendario(LocalDate hoy, List<DiaDelCalendario> dias) {

		public Calendario {
			dias = List.copyOf(dias);
		}
	}
}
