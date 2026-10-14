package co.edu.ucundinamarca.cundiapp.domain.exception;

import java.time.Duration;

/**
 * Se hicieron más peticiones de las que se permiten en la ventana (SCRUM-88). Lleva cuánto falta para
 * volver a intentar, que es lo que la persona necesita saber: «espera 3 minutos» y no «demasiados
 * intentos».
 */
public class DemasiadasPeticionesException extends RuntimeException {

	private final Duration espera;

	public DemasiadasPeticionesException(Duration espera) {
		super("Hiciste demasiadas peticiones seguidas. Espera %s e intenta de nuevo".formatted(textoDeEspera(espera)));
		this.espera = espera;
	}

	public Duration espera() {
		return espera;
	}

	/** Los segundos que hay que esperar, redondeados hacia arriba y nunca menos de uno (para `Retry-After`). */
	public static long segundosDeEspera(Duration espera) {
		long segundos = espera.toSeconds() + (espera.toNanosPart() > 0 ? 1 : 0);
		return Math.max(1, segundos);
	}

	/** «45 segundos», «1 minuto», «3 minutos»: lo que se puede leer de un vistazo. */
	public static String textoDeEspera(Duration espera) {
		long segundos = segundosDeEspera(espera);
		if (segundos < 60) {
			return segundos == 1 ? "1 segundo" : segundos + " segundos";
		}
		long minutos = (segundos + 59) / 60;
		return minutos == 1 ? "1 minuto" : minutos + " minutos";
	}
}
