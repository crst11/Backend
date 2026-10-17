package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Lo que el estudiante hizo con el pendiente: sigue abierto o ya lo cumplió.
 *
 * <p>El esquema también admite «vencido», pero <b>nunca se guarda</b>: estar vencido depende de la hora
 * y lo calculado no se guarda (regla 10). Si apareciera en la base, se lee como abierto y la situación
 * la dice la fecha ({@link Pendiente#situacionA}).
 */
public enum EstadoDePendiente {
	ABIERTO("abierto"),
	CUMPLIDO("cumplido");

	private final String valorEnBd;

	EstadoDePendiente(String valorEnBd) {
		this.valorEnBd = valorEnBd;
	}

	public String valorEnBd() {
		return valorEnBd;
	}

	public static EstadoDePendiente desdeBd(String valor) {
		return CUMPLIDO.valorEnBd.equals(valor) ? CUMPLIDO : ABIERTO;
	}
}
