package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PreferenciasDeAvisos;

/** Cambia qué avisos recibo y con cuánta anticipación (RF10, SCRUM-38). Rige desde la siguiente ronda de avisos. */
public interface CambiarMisPreferenciasDeAvisos {

	PreferenciasDeAvisos ejecutar(int idEstudiante, PreferenciasDeAvisos preferencias);
}
