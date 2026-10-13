package co.edu.ucundinamarca.cundiapp.domain.model;

/**
 * Qué clases mostrar del horario (RF04, SCRUM-26): las de una asignatura, las de una sede, o las
 * dos cosas a la vez. Un criterio nulo o en blanco no filtra por eso.
 *
 * @param asignatura el código de la asignatura
 * @param sede el nombre de la sede, comparado sin tildes ni mayúsculas
 */
public record FiltroDeHorario(String asignatura, String sede) {

	public static final FiltroDeHorario SIN_FILTRO = new FiltroDeHorario(null, null);

	public FiltroDeHorario {
		asignatura = enBlancoEsNulo(asignatura);
		sede = enBlancoEsNulo(sede);
	}

	boolean admite(ClaseDelHorario clase) {
		if (asignatura != null && !asignatura.equalsIgnoreCase(clase.codigoAsignatura())) {
			return false;
		}
		if (sede != null) {
			// Una clase sin sede no se sabe dónde es: no se cuela en el filtro de ninguna.
			String sedeDeLaClase = clase.bloque().espacio().map(EspacioFisico::sede).orElse(null);
			return sedeDeLaClase != null && TextoNormalizado.de(sedeDeLaClase).equals(TextoNormalizado.de(sede));
		}
		return true;
	}

	private static String enBlancoEsNulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.strip();
	}
}
