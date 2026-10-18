package co.edu.ucundinamarca.cundiapp.domain.service;

import co.edu.ucundinamarca.cundiapp.domain.model.NivelDeRiesgo;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Escribe lo que dice cada aviso (RF10). El texto sale del dominio y no de la pantalla para que el aviso
 * diga lo mismo dentro de la aplicación y en la notificación del dispositivo.
 *
 * <p>Las horas son de Bogotá, como en el resto de la aplicación, y se escriben a mano («11:59 p. m.»):
 * el formato del JDK cambia los espacios según la versión y un aviso no debería depender de eso.
 */
public class RedactorDeAvisos {

	/** Título y cuerpo de un aviso. */
	public record Texto(String titulo, String mensaje) {
	}

	private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
	private static final Locale ESPANOL = Locale.of("es", "CO");

	public Texto vencimiento(String tituloDelPendiente, Instant fechaLimite, Instant ahora) {
		String cuando = cuando(fechaLimite, ahora);
		// «p. m.» ya termina en punto: otro más dejaría «p. m..».
		String cierre = cuando.endsWith(".") ? "" : ".";
		return new Texto("Vence pronto", "«" + tituloDelPendiente + "» vence " + cuando + cierre);
	}

	/** «El riesgo en «Cálculo» pasó de medio a alto.» (RF10, SCRUM-50). */
	public Texto cambioDeRiesgo(String asignatura, NivelDeRiesgo antes, NivelDeRiesgo ahora) {
		String titulo = antes.empeoraAlPasarA(ahora) ? "Subió tu riesgo" : "Bajó tu riesgo";
		return new Texto(titulo,
				"El riesgo en «" + asignatura + "» pasó de " + nombre(antes) + " a " + nombre(ahora) + ".");
	}

	private static String nombre(NivelDeRiesgo nivel) {
		return switch (nivel) {
			case ALTO -> "alto";
			case MEDIO -> "medio";
			case BAJO -> "bajo";
			case SIN_DATOS -> "sin datos";
		};
	}

	/** «hoy a las 11:59 p. m.», «mañana a las 8:00 a. m.» o «el jueves 15 de octubre a las 2:30 p. m.». */
	String cuando(Instant fechaLimite, Instant ahora) {
		ZonedDateTime limite = fechaLimite.atZone(BOGOTA);
		LocalDate hoy = ahora.atZone(BOGOTA).toLocalDate();
		LocalDate dia = limite.toLocalDate();
		String hora = "a las " + hora(limite);
		if (dia.equals(hoy)) {
			return "hoy " + hora;
		}
		if (dia.equals(hoy.plusDays(1))) {
			return "mañana " + hora;
		}
		DayOfWeek semana = dia.getDayOfWeek();
		return "el " + semana.getDisplayName(TextStyle.FULL, ESPANOL) + " " + dia.getDayOfMonth() + " de "
				+ dia.getMonth().getDisplayName(TextStyle.FULL, ESPANOL) + " " + hora;
	}

	private static String hora(ZonedDateTime momento) {
		int hora12 = momento.getHour() % 12 == 0 ? 12 : momento.getHour() % 12;
		String sufijo = momento.getHour() < 12 ? "a. m." : "p. m.";
		return String.format(Locale.ROOT, "%d:%02d %s", hora12, momento.getMinute(), sufijo);
	}
}
