package co.edu.ucundinamarca.cundiapp.infrastructure.adapter.in.rest.dto;

import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarReporteDeHorario.Analisis;
import co.edu.ucundinamarca.cundiapp.application.port.in.AnalizarReporteDeHorario.AsignaturaPorImportar;
import co.edu.ucundinamarca.cundiapp.domain.model.BloqueDeHorario;
import java.util.List;

/**
 * Lo que se detectó en el Reporte de Horario, para que el estudiante lo revise antes de guardar
 * (SCRUM-81). Las clases usan los mismos nombres que {@link AgregarClaseDto}: lo que se muestra aquí
 * es lo que se manda de vuelta al confirmar, sin traducir nada.
 */
public record AnalisisDeHorarioDto(
		String nombreArchivo,
		String programaDelReporte,
		String sedeDelReporte,
		boolean coincideConMiPrograma,
		String periodo,
		int detectadas,
		int clases,
		long seVanAGuardar,
		List<AsignaturaDto> asignaturas) {

	public record AsignaturaDto(
			String codigo,
			String nombre,
			String grupo,
			boolean enElPlan,
			boolean yaEstaba,
			int clasesQueTiene,
			List<ClaseDto> clases) {
	}

	public record ClaseDto(
			String dia,
			String horaInicio,
			String horaFin,
			String salon,
			String bloque,
			String sede,
			AgregarClaseDto.DocenteDto docente) {

		static ClaseDto desde(BloqueDeHorario bloque) {
			var espacio = bloque.espacio().orElse(null);
			return new ClaseDto(
					bloque.dia().valorEnBd(),
					bloque.horaInicio().toString(),
					bloque.horaFin().toString(),
					espacio == null ? null : espacio.salon(),
					espacio == null ? null : espacio.bloque(),
					espacio == null ? null : espacio.sede(),
					bloque.docente().map(d -> new AgregarClaseDto.DocenteDto(d.nombres(), d.apellidos())).orElse(null));
		}
	}

	public static AnalisisDeHorarioDto desde(Analisis analisis, String nombreArchivo) {
		return new AnalisisDeHorarioDto(
				nombreArchivo,
				analisis.programaDelReporte(),
				analisis.sedeDelReporte(),
				analisis.coincideConMiPrograma(),
				analisis.periodo(),
				analisis.asignaturas().size(),
				analisis.clases(),
				analisis.seVanAGuardar(),
				analisis.asignaturas().stream().map(AnalisisDeHorarioDto::asignatura).toList());
	}

	private static AsignaturaDto asignatura(AsignaturaPorImportar a) {
		return new AsignaturaDto(
				a.codigo(),
				a.nombre(),
				a.grupo(),
				a.enElPlan(),
				a.yaEstaba(),
				a.clasesQueTiene(),
				a.bloques().stream().map(ClaseDto::desde).toList());
	}
}
