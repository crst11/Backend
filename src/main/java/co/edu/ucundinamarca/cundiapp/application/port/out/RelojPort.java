package co.edu.ucundinamarca.cundiapp.application.port.out;

import java.time.Instant;

/** La hora actual, como puerto: el reloj del sistema en producción, uno fijo en pruebas. */
public interface RelojPort {

	Instant ahora();
}
