package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * El árbol de evaluación como lo lee la pantalla (SCRUM-27).
 *
 * <p>{@code guardada} en falso significa que esto es la propuesta de la plantilla y que todavía no
 * hay nada escrito: la pantalla lo dice en pantalla en vez de hacerle creer que ya lo configuró.
 */
public record EstructuraDeEvaluacionDto(
		AsignaturaMatriculadaDto asignatura,
		boolean guardada,
		List<CategoriaDto> categorias) {

	public record CategoriaDto(
			int consecutivo,
			String nombre,
			BigDecimal porcentaje,
			String origen,
			List<ActividadDto> actividades) {
	}

	public record ActividadDto(
			int consecutivo,
			String nombre,
			BigDecimal porcentaje,
			LocalDate fechaProgramada,
			String tipo,
			String estadoEntrega) {
	}

	public static EstructuraDeEvaluacionDto desde(ConsultarMiEstructuraDeEvaluacion.Resultado resultado) {
		return new EstructuraDeEvaluacionDto(
				AsignaturaMatriculadaDto.desde(resultado.asignatura()),
				resultado.guardada(),
				resultado.estructura().categorias().stream().map(EstructuraDeEvaluacionDto::categoria).toList());
	}

	private static CategoriaDto categoria(CategoriaDeEvaluacion categoria) {
		return new CategoriaDto(
				categoria.consecutivo(),
				categoria.nombre(),
				categoria.porcentaje(),
				categoria.origen().valorEnBd(),
				categoria.actividades().stream().map(EstructuraDeEvaluacionDto::actividad).toList());
	}

	private static ActividadDto actividad(ActividadEvaluativa actividad) {
		return new ActividadDto(
				actividad.consecutivo(),
				actividad.nombre(),
				actividad.porcentaje(),
				actividad.fechaProgramada(),
				actividad.tipo().valorEnBd(),
				actividad.estado().valorEnBd());
	}
}
