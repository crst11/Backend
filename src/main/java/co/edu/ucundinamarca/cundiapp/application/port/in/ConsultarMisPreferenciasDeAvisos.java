package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;

/** Qué avisos recibo y con cuánta anticipación (RF10, SCRUM-38). */
public interface ConsultarMisPreferenciasDeAvisos {

	PreferenciasDeAvisos ejecutar(int idEstudiante);
}
