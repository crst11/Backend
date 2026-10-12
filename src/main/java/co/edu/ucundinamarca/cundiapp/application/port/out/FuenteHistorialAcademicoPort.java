package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.ReporteAcademicoDetectado;

/**
 * De dónde salen los datos académicos del estudiante (RF03).
 *
 * <p>Hoy el adaptador lee el PDF que la universidad le entrega al propio estudiante. Mañana, si la
 * universidad autoriza una integración, entra otro adaptador y el núcleo no se entera.
 */
public interface FuenteHistorialAcademicoPort {

	ReporteAcademicoDetectado leer(byte[] archivo);
}
