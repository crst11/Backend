package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaOfertada;
import co.edu.ucundinamarca.cundiapp.domain.model.CriterioDeAcomodo;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDeAcomodo;
import java.util.List;

/**
 * Proponer combinaciones de grupos sin choques para matricular (RF13, SCRUM-208).
 *
 * <p>La oferta la escribe el estudiante y viaja en la petición: el cálculo no guarda nada, así que no necesita
 * saber quién es. La sesión sigue siendo obligatoria, como en todo {@code /api/mis}.
 */
public interface ProponerHorarios {

	ResultadoDeAcomodo ejecutar(List<AsignaturaOfertada> oferta, CriterioDeAcomodo criterio);
}
