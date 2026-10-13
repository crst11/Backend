package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Dónde se dicta una clase: el salón, el bloque del edificio y la sede (RF04, SCRUM-25).
 *
 * <p>El estudiante lo escribe tal como lo ve en su reporte; no hay un catálogo oficial contra el
 * cual validarlo. Por eso el bloque es opcional (un auditorio no siempre lo tiene) y todo se
 * recorta: «F-203 » y «F-203» son el mismo salón.
 */
public record EspacioFisico(String salon, String bloque, String sede) {

	/** Lo que cabe en espacio_fisico.nomenclatura. */
	static final int LARGO_DEL_SALON = 30;
	/** Lo que cabe en espacio_fisico.bloque (V10): el Reporte de Horario escribe nombres de hasta 40. */
	static final int LARGO_DEL_BLOQUE = 60;
	/** Lo que cabe en espacio_fisico.sede. */
	static final int LARGO_DE_LA_SEDE = 60;

	public EspacioFisico {
		salon = recortado(salon);
		bloque = recortado(bloque);
		sede = recortado(sede);
		if (salon == null) {
			throw new ReglaDeNegocioVioladaException("Falta el salón de la clase");
		}
		if (sede == null) {
			throw new ReglaDeNegocioVioladaException("Falta la sede de la clase");
		}
		if (salon.length() > LARGO_DEL_SALON) {
			throw new ReglaDeNegocioVioladaException(
					"El salón no puede pasar de %d caracteres".formatted(LARGO_DEL_SALON));
		}
		if (bloque != null && bloque.length() > LARGO_DEL_BLOQUE) {
			throw new ReglaDeNegocioVioladaException(
					"El bloque no puede pasar de %d caracteres".formatted(LARGO_DEL_BLOQUE));
		}
		if (sede.length() > LARGO_DE_LA_SEDE) {
			throw new ReglaDeNegocioVioladaException(
					"La sede no puede pasar de %d caracteres".formatted(LARGO_DE_LA_SEDE));
		}
	}

	/** Un texto en blanco es lo mismo que no haber escrito nada. */
	private static String recortado(String texto) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		return texto.strip();
	}
}
