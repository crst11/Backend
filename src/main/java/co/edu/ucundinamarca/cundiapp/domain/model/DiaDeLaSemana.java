package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import java.time.DayOfWeek;

/**
 * Un día de la semana de clases (RF04, SCRUM-25). Refleja los valores de la restricción
 * ck_bloque_dia del esquema, sin tildes: así se guardan y así viajan por la API.
 *
 * <p>El orden de las constantes es el de la semana, de lunes a domingo, y de él depende cómo se
 * ordena el horario.
 */
public enum DiaDeLaSemana {
	LUNES("lunes"),
	MARTES("martes"),
	MIERCOLES("miercoles"),
	JUEVES("jueves"),
	VIERNES("viernes"),
	SABADO("sabado"),
	DOMINGO("domingo");

	private final String valorEnBd;

	DiaDeLaSemana(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	/** Es también lo que escribe el estudiante o manda la pantalla, así que un valor malo es un 422. */
	public static DiaDeLaSemana desdeBd(String valor) {
		for (DiaDeLaSemana dia : values()) {
			if (dia.valorEnBd.equals(valor)) {
				return dia;
			}
		}
		throw new ReglaDeNegocioVioladaException(
				"El día «%s» no es válido: usa lunes, martes, miercoles, jueves, viernes, sabado o domingo"
						.formatted(valor));
	}

	public static DiaDeLaSemana de(DayOfWeek dia) {
		// DayOfWeek va de lunes (1) a domingo (7), igual que este enum.
		return values()[dia.getValue() - 1];
	}
}
