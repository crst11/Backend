package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja los valores de la restricción ck_sesion_motivo del esquema. */
public enum MotivoDeRevocacion {
	CIERRE_SESION("cierre_sesion"),
	ROTACION("rotacion"),
	REUSO_DETECTADO("reuso_detectado"),
	EXPIRACION("expiracion");

	private final String valorEnBd;

	MotivoDeRevocacion(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static MotivoDeRevocacion desdeBd(String valor) {
		for (MotivoDeRevocacion motivo : values()) {
			if (motivo.valorEnBd.equals(valor)) {
				return motivo;
			}
		}
		throw new IllegalArgumentException("Motivo de revocación desconocido: " + valor);
	}
}
