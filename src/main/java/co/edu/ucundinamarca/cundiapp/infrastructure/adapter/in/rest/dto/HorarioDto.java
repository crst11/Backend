package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.domain.model.ClaseDelHorario;
import co.edu.ucundinamarca.cundiapp.domain.model.DiaConClases;
import co.edu.ucundinamarca.cundiapp.domain.model.HorarioSemanal;
import java.util.List;

/**
 * El horario como lo lee la pantalla (SCRUM-25): solo los días que tienen clases, de lunes a
 * domingo, y dentro de cada día las clases en orden de hora. Las horas viajan como «08:00».
 */
public record HorarioDto(int totalDeClases, List<DiaDto> dias) {

	public record DiaDto(String dia, List<ClaseDto> clases) {
	}

	public record DocenteDto(String nombres, String apellidos) {
	}

	/** El salón, el bloque y la sede van planos porque así se muestran: una sola línea de ubicación. */
	public record ClaseDto(
			int idMatricula,
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
			DocenteDto docente) {

		public static ClaseDto desde(ClaseDelHorario clase) {
			var bloque = clase.bloque();
			var espacio = bloque.espacio().orElse(null);
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
					bloque.docente().map(docente -> new DocenteDto(docente.nombres(), docente.apellidos())).orElse(null));
		}
	}

	public static HorarioDto desde(HorarioSemanal horario) {
		return new HorarioDto(horario.totalDeClases(), horario.dias().stream().map(HorarioDto::dia).toList());
	}

	private static DiaDto dia(DiaConClases dia) {
		return new DiaDto(dia.dia().valorEnBd(), dia.clases().stream().map(ClaseDto::desde).toList());
	}
}
