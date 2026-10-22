package co.edu.ucundinamarca.cundiapp.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Lo que el estudiante prefiere al acomodar su horario de matrícula (RF13, SCRUM-207): una franja del día, pocos
 * huecos entre clases y los días que quiere sin clase. Todo es opcional; lo que no pide no cuenta en el puntaje.
 *
 * <p>No se llama {@code PreferenciaDeHorario} porque esa ya existe en RF04 con otro significado: cómo se ve el
 * horario.
 */
public record CriterioDeAcomodo(FranjaDelDia franja, boolean pocosHuecos, Set<DiaDeLaSemana> diasLibres, boolean diaLibre) {

	/** Sin el modo «quiero un día libre» (SCRUM-251): lo de siempre. Con {@code diaLibre} activo, {@code diasLibres} se ignora. */
	public CriterioDeAcomodo(FranjaDelDia franja, boolean pocosHuecos, Set<DiaDeLaSemana> diasLibres) {
		this(franja, pocosHuecos, diasLibres, false);
	}

	public CriterioDeAcomodo {
		EnumSet<DiaDeLaSemana> libres = EnumSet.noneOf(DiaDeLaSemana.class);
		if (diasLibres != null) {
			libres.addAll(diasLibres);
		}
		diasLibres = Collections.unmodifiableSet(libres);
	}

	public static CriterioDeAcomodo ninguno() {
		return new CriterioDeAcomodo(null, false, null);
	}

	public boolean pideAlgo() {
		return franja != null || pocosHuecos || !diasLibres.isEmpty() || diaLibre;
	}
}
