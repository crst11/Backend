package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.exception.ReglaDeNegocioVioladaException;
import co.edu.ucundinamarca.cundiapp.domain.model.EleccionDeHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.GrupoOfertado;
import co.edu.ucundinamarca.cundiapp.domain.service.VerificadorDeCambioDeGrupo;
import co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto.ProponerHorariosDto.GrupoDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * El horario armado y los grupos que se quieren probar en lugar de uno de sus materias (RF13, SCRUM-245). Solo
 * se exige que vengan las tres piezas; que cada una sea válida lo comprueba el dominio, con el mismo mensaje
 * que al armar el horario (los grupos y bloques son los de {@link ProponerHorariosDto}). Antes de armar los
 * objetos se mira el tamaño, para que un cuerpo enorme no se convierta en millones de objetos solo para
 * rechazarlo.
 *
 * @param horario lo que la persona tiene elegido: una materia con su grupo
 * @param asignaturaACambiar la materia del horario cuyo grupo quiere cambiar
 * @param candidatos los otros grupos de esa materia, con sus bloques
 */
public record VerificarCambioDto(
		@NotNull List<EleccionDto> horario, @NotBlank String asignaturaACambiar, @NotNull List<GrupoDto> candidatos) {

	/** Una materia del horario con el grupo que tiene elegido. */
	public record EleccionDto(String asignatura, GrupoDto grupo) {
	}

	public List<EleccionDeHorario> aHorario() {
		revisarTamano();
		List<EleccionDeHorario> elecciones = new ArrayList<>();
		for (EleccionDto eleccion : horario) {
			if (eleccion == null) {
				throw new ReglaDeNegocioVioladaException("Falta una de las materias del horario");
			}
			elecciones.add(new EleccionDeHorario(eleccion.asignatura(), ProponerHorariosDto.aGrupo(eleccion.grupo())));
		}
		return elecciones;
	}

	public List<GrupoOfertado> aCandidatos() {
		revisarTamano();
		return candidatos.stream().map(ProponerHorariosDto::aGrupo).toList();
	}

	private void revisarTamano() {
		if (horario.size() > VerificadorDeCambioDeGrupo.MAXIMO_DE_ELECCIONES) {
			throw new ReglaDeNegocioVioladaException(
					"El horario no puede tener más de %d materias".formatted(VerificadorDeCambioDeGrupo.MAXIMO_DE_ELECCIONES));
		}
		if (candidatos.size() > VerificadorDeCambioDeGrupo.MAXIMO_DE_CANDIDATOS) {
			throw new ReglaDeNegocioVioladaException(
					"No se pueden probar más de %d grupos a la vez".formatted(VerificadorDeCambioDeGrupo.MAXIMO_DE_CANDIDATOS));
		}
	}
}
