package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Con qué vista abre el horario (RF04, SCRUM-26). Refleja los valores de la restricción
 * ck_configuracion_horario_vista del esquema.
 */
public enum VistaDelHorario {
	DIA("dia"),
	SEMANA("semana");

	private final String valorEnBd;

	VistaDelHorario(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static VistaDelHorario desdeBd(String valor) {
		for (VistaDelHorario vista : values()) {
			if (vista.valorEnBd.equals(valor)) {
				return vista;
			}
		}
		throw new ReglaDeNegocioVioladaException("La vista «%s» no es válida: usa dia o semana".formatted(valor));
	}
}
