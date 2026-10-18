package co.edu.ucundinamarca.cundiapp.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Los festivos de Colombia, calculados por la ley y no consultados a un servicio (RF07, SCRUM-85):
 * así funcionan sin internet y no dependen de un tercero.
 *
 * <p>Son 18 al año, aunque a veces dos caen el mismo día. **Fijos**: Año Nuevo, Día del Trabajo, Grito de Independencia, Batalla de Boyacá,
 * Inmaculada Concepción y Navidad. **Se trasladan al lunes siguiente** (Ley 51 de 1983, «ley
 * Emiliani»): Reyes Magos, San José, San Pedro y San Pablo, la Asunción, el Día de la Raza, Todos los
 * Santos y la Independencia de Cartagena; si ya caen en lunes, no se mueven. **Dependen de la Semana
 * Santa**: Jueves y Viernes Santo en su día, y Ascensión, Corpus Christi y Sagrado Corazón, que se
 * pasan al lunes de la semana siguiente a la fecha litúrgica.
 */
public final class FestivosDeColombia {

	private FestivosDeColombia() {
	}

	/** Todos los festivos del año, de enero a diciembre, con su nombre. */
	public static Map<LocalDate, String> delAnio(int anio) {
		Map<LocalDate, String> festivos = new TreeMap<>();
		agregar(festivos, LocalDate.of(anio, 1, 1), "Año Nuevo");
		agregar(festivos, LocalDate.of(anio, 5, 1), "Día del Trabajo");
		agregar(festivos, LocalDate.of(anio, 7, 20), "Grito de Independencia");
		agregar(festivos, LocalDate.of(anio, 8, 7), "Batalla de Boyacá");
		agregar(festivos, LocalDate.of(anio, 12, 8), "Inmaculada Concepción");
		agregar(festivos, LocalDate.of(anio, 12, 25), "Navidad");

		agregar(festivos, alLunes(LocalDate.of(anio, 1, 6)), "Reyes Magos");
		agregar(festivos, alLunes(LocalDate.of(anio, 3, 19)), "Día de San José");
		agregar(festivos, alLunes(LocalDate.of(anio, 6, 29)), "San Pedro y San Pablo");
		agregar(festivos, alLunes(LocalDate.of(anio, 8, 15)), "Asunción de la Virgen");
		agregar(festivos, alLunes(LocalDate.of(anio, 10, 12)), "Día de la Raza");
		agregar(festivos, alLunes(LocalDate.of(anio, 11, 1)), "Todos los Santos");
		agregar(festivos, alLunes(LocalDate.of(anio, 11, 11)), "Independencia de Cartagena");

		LocalDate pascua = domingoDePascua(anio);
		agregar(festivos, pascua.minusDays(3), "Jueves Santo");
		agregar(festivos, pascua.minusDays(2), "Viernes Santo");
		agregar(festivos, alLunes(pascua.plusDays(39)), "Ascensión del Señor");
		agregar(festivos, alLunes(pascua.plusDays(60)), "Corpus Christi");
		agregar(festivos, alLunes(pascua.plusDays(68)), "Sagrado Corazón");
		return festivos;
	}

	/** Dos festivos pueden caer el mismo día (en 2025, San Pedro y San Pablo y el Sagrado Corazón): son un solo día libre. */
	private static void agregar(Map<LocalDate, String> festivos, LocalDate fecha, String nombre) {
		festivos.merge(fecha, nombre, (uno, otro) -> uno + " y " + otro);
	}

	public static Optional<String> nombreDe(LocalDate fecha) {
		return Optional.ofNullable(delAnio(fecha.getYear()).get(fecha));
	}

	public static boolean esFestivo(LocalDate fecha) {
		return nombreDe(fecha).isPresent();
	}

	/** El lunes de esa fecha si ya es lunes, y si no el lunes siguiente. */
	private static LocalDate alLunes(LocalDate fecha) {
		int diasHastaElLunes = (DayOfWeek.MONDAY.getValue() - fecha.getDayOfWeek().getValue() + 7) % 7;
		return fecha.plusDays(diasHastaElLunes);
	}

	/** Algoritmo anónimo gregoriano (Meeus/Jones/Butcher): el domingo de Resurrección del año. */
	static LocalDate domingoDePascua(int anio) {
		int a = anio % 19;
		int b = anio / 100;
		int c = anio % 100;
		int d = b / 4;
		int e = b % 4;
		int f = (b + 8) / 25;
		int g = (b - f + 1) / 3;
		int h = (19 * a + b - d - g + 15) % 30;
		int i = c / 4;
		int k = c % 4;
		int l = (32 + 2 * e + 2 * i - h - k) % 7;
		int m = (a + 11 * h + 22 * l) / 451;
		int mes = (h + l - 7 * m + 114) / 31;
		int dia = ((h + l - 7 * m + 114) % 31) + 1;
		return LocalDate.of(anio, mes, dia);
	}
}
