package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.Aviso;
import java.util.List;

/** Mis avisos, del más reciente al más antiguo, con cuántos no he leído (RF10, SCRUM-37). */
public interface ListarMisAvisos {

	Resultado ejecutar(int idEstudiante, boolean soloSinLeer);

	/** `sinLeer` cuenta todos los no leídos, aunque la lista venga recortada. */
	record Resultado(int sinLeer, List<Aviso> avisos) {
	}
}
