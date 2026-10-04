package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.PeriodoConfirmado;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.Resultado;
import java.util.List;

/** Lo que el núcleo necesita para dejar guardada una importación confirmada (RF03). */
public interface ImportacionRepositorio {

	/**
	 * Guarda la importación entera: la fila de `importacion`, los períodos que falten, las
	 * matrículas y los totales oficiales. Todo junto o nada: a medias dejaría el historial mintiendo.
	 */
	Resultado guardar(
			int idEstudiante, String nombreArchivo, int detectadas, List<PeriodoConfirmado> periodos);
}
