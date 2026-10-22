package co.edu.ucundinamarca.cundiapp.application.service;

import co.edu.ucundinamarca.cundiapp.application.port.in.VerificarCambioDeGrupo;
import co.edu.ucundinamarca.cundiapp.domain.model.EleccionDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.model.ResultadoDelCandidato;
import co.edu.ucundinamarca.cundiapp.domain.service.VerificadorDeCambioDeGrupo;
import java.util.List;

/** Le pasa el horario y los grupos candidatos al verificador del dominio (RF13, SCRUM-245). Las reglas están allá. */
public class VerificarCambioDeGrupoServicio implements VerificarCambioDeGrupo {

	private final VerificadorDeCambioDeGrupo verificador;

	public VerificarCambioDeGrupoServicio(VerificadorDeCambioDeGrupo verificador) {
		this.verificador = verificador;
	}

	@Override
	public List<ResultadoDelCandidato> ejecutar(
			List<EleccionDeHorario> horario, String asignaturaACambiar, List<GrupoOfertado> candidatos) {
		return verificador.verificar(horario, asignaturaACambiar, candidatos);
	}
}
