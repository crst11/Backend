package co.edu.ucundinamarca.cundiapp.domain.model;

/** Refleja los valores de la restricción ck_matricula_estado del esquema. */
public enum EstadoDeAsignatura {
	EN_CURSO("en_curso"),
	APROBADA("aprobada"),
	REPROBADA("reprobada"),
	CANCELADA("cancelada");

	private final String valorEnBd;

	EstadoDeAsignatura(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static EstadoDeAsignatura desdeBd(String valor) {
		for (EstadoDeAsignatura estado : values()) {
			if (estado.valorEnBd.equals(valor)) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de asignatura desconocido: " + valor);
	}

	/** Una cancelada no cuenta en ningún lado: el estudiante la retiró a tiempo. */
	public boolean cuentaEnLaCarga() {
		return this != CANCELADA;
	}
}
