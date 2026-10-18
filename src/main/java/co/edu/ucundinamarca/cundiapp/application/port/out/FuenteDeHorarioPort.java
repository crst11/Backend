package co.edu.ucundinamarca.cundiapp.application.port.out;

import co.edu.ucundinamarca.cundiapp.domain.model.ReporteDeHorarioDetectado;

/** De dónde sale el Reporte de Horario: hoy es el PDF que entrega la universidad (RF04, SCRUM-81). */
public interface FuenteDeHorarioPort {

	/** Rechaza con un mensaje que el estudiante entienda lo que no sea ese reporte. */
	ReporteDeHorarioDetectado leer(byte[] archivo);
}
