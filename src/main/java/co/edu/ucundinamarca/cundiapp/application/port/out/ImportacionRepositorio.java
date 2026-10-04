package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.PeriodoConfirmado;
import co.edu.ucundinamarca.cundiapp.application.port.in.DeshacerImportacion;
import co.edu.ucundinamarca.cundiapp.application.port.in.ConfirmarImportacionDeRegistro.Resultado;
import co.edu.ucundinamarca.cundiapp.domain.model.Importacion;
import java.util.List;
import java.util.Optional;

/** Lo que el núcleo necesita para dejar guardada una importación confirmada (RF03). */
public interface ImportacionRepositorio {

	/**
	 * Guarda la importación entera: la fila de `importacion`, los períodos que falten, las
	 * matrículas y los totales oficiales. Todo junto o nada: a medias dejaría el historial mintiendo.
	 */
	Resultado guardar(
			int idEstudiante, String nombreArchivo, int detectadas, List<PeriodoConfirmado> periodos);

	/** Las cargas del estudiante, de la más reciente a la más antigua. */
	List<Importacion> deEstudiante(int idEstudiante);

	Optional<Importacion> buscar(int idEstudiante, int idImportacion);

	/**
	 * Devuelve el historial al estado anterior a esa carga: lo que creó se elimina y lo que pisó
	 * vuelve a su valor. La importación queda marcada como revertida.
	 */
	DeshacerImportacion.Resultado revertir(int idEstudiante, int idImportacion);
}
