package co.edu.ucundinamarca.cundiapp.domain.model;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;

/**
 * Una asignatura que el estudiante tiene matriculada, vista desde la evaluación (RF05, SCRUM-27).
 *
 * <p>Es la fila sobre la que cuelga todo el árbol de evaluación, y lo que la pantalla necesita para
 * dejarlo elegir: el nombre de la materia, el período y si ya dijo cómo lo evalúan.
 */
public record AsignaturaMatriculada(
		int idMatricula,
		String codigoAsignatura,
		String nombre,
		int creditos,
		String codigoPeriodo,
		EstadoDeAsignatura estado,
		int categoriasDefinidas,
		int actividadesDefinidas) {

	public AsignaturaMatriculada {
		if (idMatricula <= 0) {
			throw new ReglaDeNegocioVioladaException("La matrícula necesita un identificador");
		}
		if (codigoAsignatura == null || codigoAsignatura.isBlank()) {
			throw new ReglaDeNegocioVioladaException("La matrícula necesita el código de la asignatura");
		}
		if (estado == null) {
			throw new ReglaDeNegocioVioladaException("La matrícula necesita un estado");
		}
	}

	/** Mientras no tenga categorías, esta asignatura sigue con la evaluación sin configurar. */
	public boolean tieneEstructura() {
		return categoriasDefinidas > 0;
	}

	/** El simulador solo tiene sentido en lo que todavía se está cursando. */
	public boolean estaEnCurso() {
		return estado == EstadoDeAsignatura.EN_CURSO;
	}
}
