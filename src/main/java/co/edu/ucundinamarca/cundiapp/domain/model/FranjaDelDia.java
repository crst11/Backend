package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.util.Locale;

/**
 * La parte del día en la que el estudiante prefiere tener clase (RF13, SCRUM-207): mañana (antes de las
 * 12:00), tarde (de 12:00 a 18:00) o noche (desde las 18:00).
 */
public enum FranjaDelDia {
	MANANA("manana", "la mañana", 0, 12 * 60),
	TARDE("tarde", "la tarde", 12 * 60, 18 * 60),
	NOCHE("noche", "la noche", 18 * 60, 24 * 60);

	private final String valorApi;
	private final String nombre;
	private final int desdeEnMinutos;
	private final int hastaEnMinutos;

	FranjaDelDia(String valorApi, String nombre, int desdeEnMinutos, int hastaEnMinutos) {
		this.valorApi = valorApi;
		this.nombre = nombre;
		this.desdeEnMinutos = desdeEnMinutos;
		this.hastaEnMinutos = hastaEnMinutos;
	}

	/** Lo que viaja por la API, sin tildes. */
	public String valorApi() {
		return valorApi;
	}

	/** Cómo se dice en una frase: «en la mañana». */
	public String nombre() {
		return nombre;
	}

	/** Cuántos minutos de una clase, de {@code inicio} a {@code fin} (minutos desde la medianoche), caen en esta franja. */
	public int minutosQueCaenEn(int inicioEnMinutos, int finEnMinutos) {
		return Math.max(0, Math.min(finEnMinutos, hastaEnMinutos) - Math.max(inicioEnMinutos, desdeEnMinutos));
	}

	/** Es lo que escribe el estudiante o manda la pantalla, así que un valor malo es una regla de negocio. */
	public static FranjaDelDia desdeApi(String valor) {
		String limpio = valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
		for (FranjaDelDia franja : values()) {
			if (franja.valorApi.equals(limpio)) {
				return franja;
			}
		}
		throw new ReglaDeNegocioVioladaException("La franja «%s» no es válida: usa manana, tarde o noche".formatted(valor));
	}
}
