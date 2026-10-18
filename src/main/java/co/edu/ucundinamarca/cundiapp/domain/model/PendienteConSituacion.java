package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Un pendiente con cómo está ahora mismo. Es lo que se le muestra al estudiante (RF06, SCRUM-30). */
public record PendienteConSituacion(Pendiente pendiente, SituacionDelPendiente situacion) {

	public static PendienteConSituacion de(Pendiente pendiente, Instant ahora) {
		return new PendienteConSituacion(pendiente, pendiente.situacionA(ahora));
	}

	/**
	 * Del más próximo al más lejano: los vencidos quedan arriba porque su fecha es la más vieja y son lo
	 * que hay que atender primero; con la misma fecha va primero la prioridad más alta; y los ya
	 * cumplidos, al final.
	 */
	public static List<PendienteConSituacion> ordenar(List<Pendiente> pendientes, Instant ahora) {
		Comparator<PendienteConSituacion> orden = Comparator
				.comparing((PendienteConSituacion p) -> p.situacion() == SituacionDelPendiente.CUMPLIDO)
				.thenComparing(p -> p.pendiente().fechaLimite())
				.thenComparing(p -> p.pendiente().prioridad())
				.thenComparing(p -> p.pendiente().id(), Comparator.nullsLast(Comparator.naturalOrder()));
		return pendientes.stream().map(p -> de(p, ahora)).sorted(orden).toList();
	}
}
