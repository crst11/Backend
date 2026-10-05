package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.AsignaturaMatriculada;

/** Una asignatura matriculada como la lista la pantalla de evaluación (SCRUM-27). */
public record AsignaturaMatriculadaDto(
		int idMatricula,
		String codigoAsignatura,
		String nombre,
		int creditos,
		String codigoPeriodo,
		String estado,
		boolean enCurso,
		boolean tieneEstructura,
		int categorias,
		int actividades) {

	public static AsignaturaMatriculadaDto desde(AsignaturaMatriculada asignatura) {
		return new AsignaturaMatriculadaDto(
				asignatura.idMatricula(),
				asignatura.codigoAsignatura(),
				asignatura.nombre(),
				asignatura.creditos(),
				asignatura.codigoPeriodo(),
				asignatura.estado().valorEnBd(),
				asignatura.estaEnCurso(),
				asignatura.tieneEstructura(),
				asignatura.categoriasDefinidas(),
				asignatura.actividadesDefinidas());
	}
}
