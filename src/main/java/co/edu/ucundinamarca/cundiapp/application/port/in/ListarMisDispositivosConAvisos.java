package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.DispositivoConAvisos;
import java.util.List;

/** Los dispositivos que hoy reciben mis avisos (RF10, SCRUM-38). */
public interface ListarMisDispositivosConAvisos {

	List<DispositivoConAvisos> ejecutar(int idEstudiante);
}
