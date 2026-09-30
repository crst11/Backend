package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.RecursoInstitucional;
import java.time.LocalDate;

/**
 * Un documento de la guía tal como lo muestra la app: su enlace oficial, si es un archivo que se
 * descarga (y en qué formato) o una página, y si sigue vigente con la fecha en que se verificó.
 */
public record RecursoInstitucionalDto(
		Integer id,
		String titulo,
		String descripcion,
		String url,
		String tipo,
		String formato,
		boolean descargable,
		boolean vigente,
		LocalDate fechaVerificacion,
		CategoriaDeRecursoDto categoria) {

	public static RecursoInstitucionalDto desde(RecursoInstitucional recurso) {
		String formato = recurso.formatoDeArchivo().orElse(null);
		return new RecursoInstitucionalDto(
				recurso.id(),
				recurso.titulo(),
				recurso.descripcion(),
				recurso.url(),
				recurso.tipo().valorEnBd(),
				formato,
				formato != null,
				recurso.estaVigente(),
				recurso.fechaVerificacion(),
				CategoriaDeRecursoDto.desde(recurso.categoria()));
	}
}
