package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiEstructuraDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.ActividadEvaluativa;
import co.edu.ucundinamarca.cundiapp.domain.model.CategoriaDeEvaluacion;
import co.edu.ucundinamarca.cundiapp.domain.model.NotaAcumulada;
import co.edu.ucundinamarca.cundiapp.domain.model.NotasDeLaAsignatura;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * El árbol de evaluación como lo lee la pantalla (SCRUM-27).
 *
 * <p>Con SCRUM-28 trae también la nota de cada actividad (nula si no tiene) y lo que el estudiante
 * lleva acumulado. Esos números se calculan en el dominio: la pantalla solo los muestra.
 *
 * <p>{@code guardada} en falso significa que esto es la propuesta de la plantilla y que todavía no
 * hay nada escrito: la pantalla lo dice en pantalla en vez de hacerle creer que ya lo configuró.
 */
public record EstructuraDeEvaluacionDto(
		AsignaturaMatriculadaDto asignatura,
		boolean guardada,
		List<CategoriaDto> categorias,
		AcumuladoDto acumulado) {

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
			String estadoEntrega,
			BigDecimal nota) {
	}

	/** Lo que lleva el estudiante (SCRUM-28). {@code promedioDeLoEvaluado} es nulo mientras no haya notas. */
	public record AcumuladoDto(
			BigDecimal puntos,
			BigDecimal porcentajeEvaluado,
			BigDecimal promedioDeLoEvaluado,
			int actividadesCalificadas,
			int actividadesTotales) {

		public static AcumuladoDto desde(NotaAcumulada acumulada) {
			return new AcumuladoDto(
					acumulada.puntos(),
					acumulada.porcentajeEvaluado(),
					acumulada.promedioDeLoEvaluado().orElse(null),
					acumulada.actividadesCalificadas(),
					acumulada.actividadesTotales());
		}
	}

	public static EstructuraDeEvaluacionDto desde(ConsultarMiEstructuraDeEvaluacion.Resultado resultado) {
		return new EstructuraDeEvaluacionDto(
				AsignaturaMatriculadaDto.desde(resultado.asignatura()),
				resultado.guardada(),
				resultado.estructura().categorias().stream()
						.map(categoria -> categoria(categoria, resultado.notas()))
						.toList(),
				AcumuladoDto.desde(resultado.acumulado()));
	}

	private static CategoriaDto categoria(CategoriaDeEvaluacion categoria, NotasDeLaAsignatura notas) {
		return new CategoriaDto(
				categoria.consecutivo(),
				categoria.nombre(),
				categoria.porcentaje(),
				categoria.origen().valorEnBd(),
				categoria.actividades().stream()
						.map(actividad -> actividad(categoria.consecutivo(), actividad, notas))
						.toList());
	}

	private static ActividadDto actividad(int consecCategoria, ActividadEvaluativa actividad, NotasDeLaAsignatura notas) {
		return new ActividadDto(
				actividad.consecutivo(),
				actividad.nombre(),
				actividad.porcentaje(),
				actividad.fechaProgramada(),
				actividad.tipo().valorEnBd(),
				actividad.estado().valorEnBd(),
				notas.de(consecCategoria, actividad.consecutivo()).map(nota -> nota.valor()).orElse(null));
	}
}
