package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.ConsultarMiHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaConClases;
import java.util.List;

/**
 * El horario como lo lee la pantalla (SCRUM-25): solo los días que tienen clases, de lunes a
 * domingo, y dentro de cada día las clases en el orden pedido (por hora, por defecto). Las horas viajan
 * como «08:00».
 *
 * <p>Con SCRUM-26 trae también lo que se puede elegir en los filtros ({@code sedes} y {@code asignaturas},
 * siempre del horario completo) y el filtro y el orden que se aplicaron, para que la pantalla muestre
 * lo que de verdad se le pidió al servidor.
 */
public record HorarioDto(
		int totalDeClases,
		List<DiaDto> dias,
		List<String> sedes,
		List<AsignaturaDto> asignaturas,
		AplicadoDto aplicado) {

	public record AsignaturaDto(String codigo, String nombre) {
	}

	/** El filtro y el orden con los que se armó este horario; el criterio nulo es «todas». */
	public record AplicadoDto(String asignatura, String sede, String orden) {
	}

	public record DiaDto(String dia, List<ClaseDto> clases) {
	}

	public record DocenteDto(String nombres, String apellidos) {
	}

	/**
	 * El salón, el bloque y la sede van planos porque así se muestran: una sola línea de ubicación.
	 *
	 * <p>Una actividad extra (SCRUM-266) no es de ninguna matrícula: {@code idMatricula} y
	 * {@code codigoAsignatura} vienen nulos, {@code asignatura} trae su nombre, y el lugar y el responsable van
	 * como texto libre en {@code lugar} y {@code responsable} (no son un salón con sede ni un docente).
	 */
	public record ClaseDto(
			Integer idMatricula,
			int consecutivo,
			String codigoAsignatura,
			String asignatura,
			String dia,
			String horaInicio,
			String horaFin,
			long duracionEnMinutos,
			String salon,
			String bloque,
			String sede,
			DocenteDto docente,
			boolean esExtra,
			Integer idActividadExtra,
			String lugar,
			String responsable) {

		public static ClaseDto desde(ClaseDelHorario clase) {
			var bloque = clase.bloque();
			var espacio = bloque.espacio().orElse(null);
			var extra = clase.extra();
			return new ClaseDto(
					clase.idMatricula(),
					bloque.consecutivo(),
					clase.codigoAsignatura(),
					clase.asignatura(),
					bloque.dia().valorEnBd(),
					bloque.horaInicio().toString(),
					bloque.horaFin().toString(),
					bloque.duracionEnMinutos(),
					espacio == null ? null : espacio.salon(),
					espacio == null ? null : espacio.bloque(),
					espacio == null ? null : espacio.sede(),
					bloque.docente().map(docente -> new DocenteDto(docente.nombres(), docente.apellidos())).orElse(null),
					clase.esExtra(),
					extra == null ? null : extra.id(),
					extra == null ? null : extra.lugar(),
					extra == null ? null : extra.responsable());
		}
	}

	public static HorarioDto desde(ConsultarMiHorario.Resultado resultado) {
		var horario = resultado.horario();
		return new HorarioDto(
				horario.totalDeClases(),
				horario.dias(resultado.orden()).stream().map(HorarioDto::dia).toList(),
				resultado.sedes(),
				resultado.asignaturas().stream().map(a -> new AsignaturaDto(a.codigo(), a.nombre())).toList(),
				new AplicadoDto(resultado.filtro().asignatura(), resultado.filtro().sede(), resultado.orden().valorEnBd()));
	}

	private static DiaDto dia(DiaConClases dia) {
		return new DiaDto(dia.dia().valorEnBd(), dia.clases().stream().map(ClaseDto::desde).toList());
	}
}
