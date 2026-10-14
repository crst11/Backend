package co.edu.ucundinamarca.cundiapp.application.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Cuenta cuántas veces se hace algo en una ventana de tiempo y avisa cuándo ya fue demasiado (SCRUM-88).
 * A diferencia de {@link LimitadorDeIntentosPort}, que cuenta fallos seguidos, este cuenta todas las
 * peticiones: sirve para lo que es caro aunque salga bien, como leer un PDF.
 */
public interface LimitadorDePeticionesPort {

	/**
	 * Cuenta esta petición. Si ya se pasó del máximo de la ventana, devuelve cuánto falta para que se
	 * libere; si todavía cabe, devuelve vacío.
	 */
	Optional<Duration> esperaSiSeExcede(String clave, int maximo, Duration ventana);
}
