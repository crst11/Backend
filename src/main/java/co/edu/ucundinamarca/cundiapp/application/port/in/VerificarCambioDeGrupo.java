package co.edu.ucundinamarca.cundiapp.application.port.in;

import co.edu.ucundinamarca.cundiapp.domain.model.EleccionDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDelCandidato;
import java.util.List;

/**
 * Verificar si cambiar una materia del horario armado por otro grupo choca con el resto (RF13, SCRUM-245).
 *
 * <p>El horario y los grupos viajan en la petición: el cálculo no guarda nada, así que no necesita saber quién
 * es. La sesión sigue siendo obligatoria, como en todo {@code /api/mis}.
 */
public interface VerificarCambioDeGrupo {

	List<ResultadoDelCandidato> ejecutar(
			List<EleccionDeHorario> horario, String asignaturaACambiar, List<GrupoOfertado> candidatos);
}
