package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.PendienteConSituacion;
import java.util.List;

/**
 * Mis pendientes del más próximo al más lejano, con los que vencen en menos de 48 horas marcados como
 * urgentes (RF06, SCRUM-30). Sin los ya cumplidos, a menos que se pidan.
 */
public interface ListarMisPendientes {

	List<PendienteConSituacion> ejecutar(int idEstudiante, boolean incluirCumplidos);
}
