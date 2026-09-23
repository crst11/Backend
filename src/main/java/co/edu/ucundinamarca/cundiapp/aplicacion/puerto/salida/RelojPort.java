package co.edu.ucundinamarca.cundiapp.aplicacion.puerto.salida;

import java.time.Instant;

/** La hora actual, como puerto: el reloj del sistema en producción, uno fijo en pruebas. */
public interface RelojPort {

	Instant ahora();
}
