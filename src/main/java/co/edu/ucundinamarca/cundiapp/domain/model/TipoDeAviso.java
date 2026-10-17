package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Los avisos que la aplicación sabe generar (RF10). La base de datos admite además
 * {@code novedad_registrada}, que ninguna historia pide todavía y por eso no se modela.
 */
public enum TipoDeAviso {
	/** Un pendiente está por vencer (SCRUM-37). */
	VENCIMIENTO_PROXIMO("vencimiento_proximo"),
	/** El riesgo de una asignatura cambió de nivel (SCRUM-50). */
	CAMBIO_RIESGO("cambio_riesgo");

	private final String valorEnBd;

	TipoDeAviso(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static TipoDeAviso desdeBd(String valor) {
		for (TipoDeAviso tipo : values()) {
			if (tipo.valorEnBd.equals(valor)) {
				return tipo;
			}
		}
		throw new ReglaDeNegocioVioladaException("No conocemos ese tipo de aviso");
	}
}
